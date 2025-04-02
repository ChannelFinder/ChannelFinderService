.. _aa_processor:
Archiver Appliance Processor
============================

.. _aa_processor_config:
Configuration
-------------
To enable the archiver appliance configuration processor, set the property :ref:`aa.enabled` to **true**.

A list of archiver appliance URLs and aliases. ::

    aa.urls={'default': 'http://archiver-01.example.com:17665', 'neutron-controls': 'http://archiver-02.example.com:17665'}

To set the choice of default archiver appliance, set the property :ref:`aa.default_alias` to the alias of the default archiver appliance. This setting can also be a comma-separated list if you want multiple default archivers.

To pass the PV as "pva://PVNAME" to the archiver appliance, set the property :ref:`aa.pva` to **true**.

The properties checked for setting a PV to be archived are ::

    aa.archive_property_name=archive
    aa.archiver_property_name=archiver

To set the auto pause behaviour, configure the parameter :ref:`aa.auto_pause`. Set to pvStatus to pause on pvStatus=Inactive,
and resume on pvStatus=Active. Set to archive to pause on archive_property_name not existing. Set to both to pause on pvStatus=Inactive and archive_property_name::

    aa.auto_pause=pvStatus,archive

AA Plugin Example
-----------------

A common use case for the archiver appliance processor is for sites that use the Recsync project to populate Channel Finder.
With the reccaster module, info tags in the IOC database specify the archiving parameters and these properties will be pushed to Channel Finder by the recceiver service.

In the example database below, the AA plugin will make requests to archive each PV.
The plugin will request MyPV to be archived with the SCAN method and sampling rate of 10 seconds to the "aa_appliance0" instance specified in aa.urls property.
MyPV2 will use the MONITOR method and a sampling rate of 0.1 seconds, and the request will be sent to the URL mapped to the the "aa_appliance1: key.
MyPolicyPV shows an example that uses an archiver appliance "Named Policy" string and also uses the URL specified in the aa.default_alias property since the "archiver" tag is missing.

For named policy PVs, the AA plugin will first check that the named policy exists in the appliance using the getPolicyList BPL endpoint.

.. code-block::

   record(ao, "MyPV") {
       info(archive,  "scan@10")
       info(archiver, "aa_appliance0")
   }
   record(ao, "MyPV2") {
      info(archive,  "monitor@0.1")
      info(archiver, "aa_appliance1")
   }
   record(ao, "MyPVWithMultipleArchivers") {
      info(archive,  "monitor@0.1")
      info(archiver, "aa_appliance0,aa_appliance1")
   }
   record(ao, "MyPolicyPV") {
      info(archive,  "AAPolicyName")
      # no archiver tag so PV sent to archiver in aa.default_alias
   }
