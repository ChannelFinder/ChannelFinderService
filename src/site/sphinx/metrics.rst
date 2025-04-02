.. _metrics:
Metrics
=======

Metrics can be exposed by setting the `management.endpoints.web.exposure.include=prometheus` property.

.. code-block::

    management.endpoints.web.exposure.include=prometheus, metrics, health, info

Adding the prometheus property will expose the prometheus endpoint which can be scraped by prometheus.

The default metrics exposed by specifying "metrics" are:

.. code-block::

    cf.total.channel.count - Count of all ChannelFinder channels
    cf.property.count - Count of all Property Names
    cf.tag.count - Count of all tags

Tag Metrics
-----------

You can also set the metrics.tags to add counts of number of channels per tag. These are exposed as
`cf.tag_on_channels.count{tag=tagName}`. For example

.. code-block::

    metrics.tags=Accelerator, Beamline1, Beamline2, Beamline3

Would produce metrics:

.. code-block::

    cf.tag_on_channels.count=109
    cf.tag_on_channels.count{tag=Accelerator} = 100
    cf.tag_on_channels.count{tag=Beamline1} = 3
    cf.tag_on_channels.count{tag=Beamline2} = 3
    cf.tag_on_channels.count{tag=Beamline3} = 3

Property Metrics
----------------

You can also set the metrics.properties to add counts of number of channels per property and value. These are exposed as
`cf_channels_count{prop0=propValueA, prop1=propValueB}`. For example:


.. code-block::

    metrics.properties=pvStatus:Active, Inactive; archive: default, !*; archiver: beam, acc, !*

Would produce metrics:

.. code-block::

    cf_channel_count{archive="-",archiver="beam",pvStatus="Inactive",} 0.0
    cf_channel_count{archive="default",archiver="beam",pvStatus="Inactive",} 0.0
    cf_channel_count{archive="-",archiver="beam",pvStatus="Active",} 0.0
    cf_channel_count{archive="-",archiver="-",pvStatus="Active",} 2.0
    cf_channel_count{archive="default",archiver="acc",pvStatus="Inactive",} 0.0
    cf_channel_count{archive="-",archiver="acc",pvStatus="Active",} 0.0
    cf_channel_count{archive="default",archiver="-",pvStatus="Inactive",} 0.0
    cf_channel_count{archive="default",archiver="-",pvStatus="Active",} 0.0
    cf_channel_count{archive="default",archiver="beam",pvStatus="Active",} 0.0
    cf_channel_count{archive="-",archiver="acc",pvStatus="Inactive",} 0.0
    cf_channel_count{archive="-",archiver="-",pvStatus="Inactive",} 1.0
    cf_channel_count{archive="default",archiver="acc",pvStatus="Active",} 0.0

Here you can see the value "!*" is special, it does a negation on property name. So for a property name "a", using the
property value "!*" provides the count of all the channels where the property does not exist.

Note that "!someValue" is also special in that it will search for all the channels where the property is set to
something other than "someValue".
