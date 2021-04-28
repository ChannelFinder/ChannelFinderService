Configuration
=============

Configuration is performed primarily through the application.properties file.
Default configuration is `built in <https://github.com/ChannelFinder/ChannelFinder-SpringBoot/tree/master/src/main/resources>`_,
which may be suitable for initial testing and evaluation.
This may be overridden when starting the service.  eg. ::

    java -jar ChannelFinder-4.0.0.jar -Dspring.config.location=file:./application.properties

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
