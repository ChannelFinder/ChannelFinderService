---
status: proposed
date: 2026-11-26
---

# Let ChannelFinder work out which channels are live

## Context and Problem Statement

ChannelFinder is a plain data store. It keeps the channels it is given, and it does nothing to work out which of them are still reported as live. That work is left to each program that writes to ChannelFinder, which in this text is called a producer. The main one is recCeiver, which keeps no state between restarts. To work out what has changed, recCeiver has to ask ChannelFinder what was written earlier, compare that with what the IOCs report now, and write the result back. By default it also runs a clean when it starts and when it stops, which means that it marks all of its channels `Inactive` in one operation. Many of the bugs in this area come from this reading and writing back. Other sites have written their own producers, and each of those has had to solve the same problem again in its own way.

Three problems cannot be fixed by any producer. A producer cannot do anything after it has crashed, so the channels of an IOC that stops while recCeiver is down stay `Active` until recCeiver starts again and runs its clean. No producer can see what the others have written. The channel of a PV that is served by IOC A through one recCeiver and by IOC B through another is marked `Inactive` when IOC B disconnects, even though IOC A still serves it. And a producer cannot tell that somebody else changed a channel. ChannelFinder has no conditional write, so a producer that reads a channel and then writes it, as recCeiver does every time an IOC sends a list of records, can undo what another producer, a person or a script wrote in the meantime.

## Considered Options

* **A. Fix each producer.** This fixes the problems that each producer can fix for itself. It does not fix a crashed producer, a PV on two or more IOCs or a lost write, and each fix has to be made once for every producer.
* **B. A producer that keeps its state on disk.** It would still have the three problems, because none of them comes from the producer forgetting. It would also have a second store that has to agree with ChannelFinder.
* **C. Add a conditional write to ChannelFinder.** This fixes a lost write. It does not help with a crashed producer, and getting a PV on two or more IOCs right would depend on every producer following the same convention.
* **D. Producers report, and ChannelFinder marks channels `Inactive`.** ChannelFinder would still keep one status and one set of values for each channel. As today, a channel would record only the source that wrote it last. This fixes a crashed producer, and it removes the reading and writing back and the clean. It does not fix a PV on two or more IOCs, because ChannelFinder would not know that another IOC still serves the PV.
* **E. ChannelFinder keeps what each source reported.** This is option D, with the addition that ChannelFinder keeps the list of each source and works out the channel from all of them.

## Decision Outcome

Chosen option: E, because it fixes a crashed producer and a PV on two or more IOCs, and it fixes a lost write for the producers that opt in. Option D is a step towards option E.

Producers only report what they see, and ChannelFinder keeps track of the reports and works out from them which channels are live. A source is one thing that serves PVs, the way a producer knows it, and usually that is an IOC. A producer registers with ChannelFinder under a name, and it asks for a timeout. For each of its sources, the producer sends the complete list of channels with their property values. A producer that runs all the time, such as recCeiver, also sends a heartbeat that names the sources it has at that moment.

A channel is reported as live when at least one source reports it. `pvStatus` holds the answer as `Active` or `Inactive`. It is always worked out from the reports, and a registered producer never writes it. A source stops counting when its producer says that the source is gone, or no longer names it in a heartbeat. It also stops counting when its producer says that it is leaving, and when its producer is recorded as gone because the timeout has ended.

Each producer opts in for itself by registering. The current API keeps working as it does today, and ChannelFinder does not work out `pvStatus` for the channels that are only written through it.

### Consequences

The decision changes what a site sees once its producers opt in. No producer has to clean up after a restart, so the faults that come from the clean go away. The channel of a PV that is served by two or more IOCs stays `Active` for as long as one of them is reported. A channel that only one producer reports becomes `Inactive` when that producer stops or crashes, even if the IOC still runs. A channel that no source reports any more stays in ChannelFinder as `Inactive`.

Registered producers never read a channel and write it back. ChannelFinder also knows where each property came from. It can remove a property that an IOC no longer reports, and leave alone a property that a person added.

Processors that act on `pvStatus`, such as the Archiver Appliance processor with `aa.auto_pause`, will need reviewing. A channel can become `Inactive` while its IOC still runs, and a processor is sent a channel when its `pvStatus` changes.

Today ChannelFinder knows nothing about its producers, and this changes that. ChannelFinder only knows that a producer has promised to keep reporting, and whether it did. It still does not know what recsync or an IOC is, and a site's own script can be a producer in the same way as a recCeiver.

The main cost is new code in ChannelFinder, and code in each producer to register, send heartbeats and send its lists. ChannelFinder also has to store a list for each source. What that costs at the size of the largest sites has not been measured.

A site that does not opt in has nothing to migrate.
