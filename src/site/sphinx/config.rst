Configuration
=============

Configuration is performed primarily through the application.properties file.
Default configuration is `built in <https://github.com/ChannelFinder/ChannelFinder-SpringBoot/tree/master/src/main/resources>`_,
which may be suitable for initial testing and evaluation.
This may be overridden when starting the service.  eg. ::

    java -jar ChannelFinder-4.7.0.jar -Dspring.config.location=file:./application.properties

application.properties
----------------------

The following describe valid keys in an application.properties.


Server
^^^^^^

HTTP Config
"""""""""""
    server.port - HTTPS port for Channel Finder API

    server.http.enable - true/false to toggle HTTP access

    server.http.port - HTTP port for Channel Finder API

SSL Config
""""""""""

    server.ssl.key-store - Path to SSL keystore file


LDAP Client
^^^^^^^^^^^

.. _conf-embedded_ldap.enabled:

embedded_ldap.enabled
"""""""""""""""""""""

When **true** use :ref:`ldap-embedded`.

.. _conf-embedded_ldap.urls:

embedded_ldap.urls
""""""""""""""""""

TODO

.. _conf-admin-groups:

.. _conf-channel-groups:

.. _conf-property-groups:

.. _conf-tag-groups:

Role Mapping
^^^^^^^^^^^^

Comma ',' separated lists of LDAP group names. ::

    admin-groups=cf-admins,sys-admins,ADMIN
    channel-groups=cf-channels,USER
    property-groups=cf-properties,USER
    tag-groups=cf-tags,USER

.. _ldap-embedded:

Embedded LDAP Server
^^^^^^^^^^^^^^^^^^^^

When :ref:`conf-embedded_ldap.enabled` is **true**,
An LDAP server is run by the channelfinder service process and is initially populated
with entries read from the file referenced by :ref:`conf-embedded_ldap.urls`.

Elastic Search
^^^^^^^^^^^^^^

HTTP Config
"""""""""""
    elasticsearch.host_urls - Comma-separated list of URLs for the Elasticsearch hosts. All hosts listed here must belong to the same Elasticsearch cluster.

    elasticsearch.query.size - Maximum size of elasticsearch queries. WARNING this property is used to update elastic maxResultWindow size. UPDATE  with care.

    elasticsearch.create.indices - true/false to enable Channel Finder to automatically create elastic search indicies

SSL Config
""""""""""

    server.ssl.key-store - Path to SSL keystore file

Archiver Appliance Configuration Processor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
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
"""""""""""""""""

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


EPICS PV Access Server
----------------------

ChannelFinder provides an EPICS PV Access Server to access the api through pvAccess.
There are a number of options that can be set such EPICS_PVA_ADDR_LIST. To see the
full list go to `PVASettings javadoc <https://javadoc.io/doc/org.phoebus/core-pva/latest/org/epics/pva/PVASettings.html>`_.

Since it is common to run ChannelFinder inside a docker container which by default does not support IPv6 you may have
error messages in the logs about launching the EPICS PV Access service. If you only wish to have the EPICS Service available on
IPv4 you can set the environment variable

    EPICS_PVAS_INTF_ADDR_LIST="0.0.0.0"

Or to not have the EPICS PV Access Server listen, then:

    EPICS_PVAS_INTF_ADDR_LIST="0.0.0.0"

Metrics
^^^^^^^

Metrics can be exposed by setting the `management.endpoints.web.exposure.include=prometheus` property. 

.. code-block::

    management.endpoints.web.exposure.include=prometheus, metrics, health, info

Adding the prometheus property will expose the prometheus endpoint which can be scraped by prometheus.

You can also set the metrics.tags to add counts of number of channels per tag. These are exposed as
`cf_channel_count{tag=tagName}` 

.. code-block::

    metrics.tags=Accelerator, Beamline, Beamline1, Beamline2, Beamline3

You can also set the metrics.properties to add counts of number of channels per property and value. These are exposed as
`cf_propertyName_channels_count{propertyName=propertyValue}`. 


.. code-block::

    metrics.properties=pvStatus:Active, Inactive; archive: default, fast, slow; archiver: aa_beamline, aa_acccelerator