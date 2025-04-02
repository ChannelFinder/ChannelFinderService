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

Archiver Appliance Processor Configuration
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

See :ref:`_aa_processor_config`.

Metrics
^^^^^^^

See :ref:`_metrics`.


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
