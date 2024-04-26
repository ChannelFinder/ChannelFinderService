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

HTTP Server
^^^^^^^^^^^

TODO

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
--------------------

When :ref:`conf-embedded_ldap.enabled` is **true**,
An LDAP server is run by the channelfinder service process and is initially populated
with entries read from the file referenced by :ref:`conf-embedded_ldap.urls`.

Archiver Appliance Configuration Processor
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
To enable the archiver appliance configuration processor, set the property :ref:`aa.enabled` to **true**.

A list of archiver appliance URLs and aliases. ::

    aa.urls={'default': 'http://archiver-01.example.com:17665', 'neutron-controls': 'http://archiver-02.example.com:17665'}

To set the choice of default archiver appliance, set the property :ref:`aa.default_alias` to the alias of the default archiver appliance.

To pass the PV as "pva://PVNAME" to the archiver appliance, set the property :ref:`aa.pva` to **true**.

The properties checked for setting a PV to be archived are ::

    aa.archive_property_name=archive
    aa.archiver_property_name=archiver


To set the auto pause behaviour, configure the parameter :ref:`aa.auto_pause`. Set to pvStatus to pause on pvStatus=Inactive,
and resume on pvStatus=Active. Set to archive to pause on archive_property_name not existing. Set to both to pause on pvStatus=Inactive and archive_property_name::

    aa.auto_pause=pvStatus,archive


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

