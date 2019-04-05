ChannelFinder – Enhanced Directory Service
API Description
===============

Directory Data Structure
------------------------

The directory contains directory entries.
Each directory entry consists of a channel name, an arbitrary set of properties (name-value pairs), and an arbitrary set of tags (names).
Each of these elements has an owner group, which can be set by the user that created the element.
All names and values are strings.
Channel names, tags, property names, and owner (group) names are all case insensitive.


Service Type
------------

The ChannelFinder service is implemented as a REST style web service, which – in this context – means:- 

•  The URL specifies the data element that the operation works upon.
•  The HTTP method specifies the type of operation.

| GET: retrieve or query, does not modify data
| PUT: create or update, replacing the addressed element
| POST: create or update subordinates of the addressed element
| DELETE: delete the addressed element

All operations are idempotent, i.e. when repeatedly applying the identical operation, only the first execution will change the database.

•  The payload (HTTP body) always contains a representation of data.
•  See http://en.wikipedia.org/wiki/Representational_State_Transfer for a detailed discussion.

Directory data can be uploaded and retrieved in XML or JSON notation, the client specifies the type using standard HTTP headers (“Content-Type”, “Accepts”).

XML Representation
------------------

Table 1 and Table 2 show the XML and JSON representations of directory entries, i.e. the payload format of the web service transactions.



Web Service URLs and Operations
-------------------------------

Channel Resources
-----------------

**Retrieve a Channel** 

**.../channels/<name>**

Method: GET		Returns: Single Channel		Required Role: None

Return the full listing of a single channel with the given name.

**List Channels / Query by Pattern**

**.../channels?prop1=patt1&prop2=patt2&~tag=patt3&~name=patt4...**
1. 
Method: GET    Returns: List of Channels    Required Role: None

Return the list of channels which match all given expressions, i.e. the expressions are combined in a logical AND.
There are three types of expressions:

1. Value wildcards: <name>=<pattern>
True if a channel has a property with the given name, and its value matches the given pattern. Multiple expressions for the same property name are combined in a logical OR.

2. Tag name wildcards: ~tag=<pattern>
True if a channel has a tag or property whose name matches the given pattern.

3. Channel name wildcards: ~name=<pattern>
True if a channel name matches the given pattern.

Special keywords, e.g. “~tag” and “~name” for tag and channel name matches, have to start with the tilde character, else they are treated as property names in a value wildcard expression.
The patterns may contain file glob wildcard characters, i.e. “?” for a single character and “*” for any number of characters.

If called without URL parameters, the operation lists all channels in the directory.

**Examples:**

**.../channels?domain=storage+ring&element=*+corrector&type=readback**

Returns a list of all readback channels for storage ring correctors.

**.../channels?cell=14&type=setpoint&~tag=archived**

Returns a list of all archived setpoint channels in cell 14.

**.../channels?~name=SR:C01-MG:G02A%3CQDP:H2%3EFld:***

Returns a list of all channels whose names start with “SR:C01-MG:G02A<QDP:H2>Fld:”.

Note that a number of special characters need to be escaped in URL expressions – in most cases the browser or API library will do the escaping.

**Create/Replace Channel**

.../channels/<name>

Method: PUT     Payload: Single Channel      Required Role: ChannelMod

Create or completely replace the existing channel name with the payload data. If the channel exists, the authenticated user is required to be a member of its owner group. (Administrator role overrides this restriction.)

**Create/Replace Multiple Channels**

.../channels

Method: PUT     Payload: List of Channels	 Required Role: ChannelMod

Add the channels in the payload to the directory. Existing channels are replaced by the payload data. For all channels that are to be replaced or added, the authenticated user is required to be a member of their owner group. (Administrator role overrides this restriction.)

**Update Channel**

.../channels/<name>

Method: POST    Payload: Single Channel      Required Role: ChannelMod

Merge properties and tags of the channel identified by the payload into an existing channel. If the channel exists, the authenticated user is required to be a member of its owner group. (Administrator role overrides this restriction.)

**Update Channels**

.../channels

Method: POST 	Payload: List of Channels	 Required Role: ChannelMod

Merge properties and tags of the channels identified by the payload into existing channels. If the channels exist, the authenticated user is required to be a member of their owner groups. (Administrator role overrides this restriction.)

**Delete a Channel**

**.../channels/<name>**

Method: DELETE						         Required Role: ChannelMod

Delete the existing channel name and all its properties and tags.

The authenticated user must be a member of the group that owns the channel to be deleted. (Administrator role overrides this restriction.)

Property Resources
-----------------

**Retrieve a Property** 

**.../properties/<name>**

Method: GET		Returns: Single Property     Required Role: None

Return the property with the given name, listing all channels with that property in an embedded
<channels> structure.

**List Properties**

**.../properties**

Method: GET    Returns: List of Properties   Required Role: None

Return the list of all properties in the directory.

**Create/Update a Property**

.../properties/<name>

Method: PUT     Payload: Single Property     Required Role: PropertyMod

Create or completely replace the existing property name with the payload data. If the payload contains
an embedded <channels> list, the property is added to all channels in that list. In this case, the value for
each property instance is taken from the property definition inside the channel in the embedded channel
list. The property is set exclusively on all channels in the payload data, removing it from all channels
that are not included in the payload. Existing property values are replaced by the payload data.

The authenticated user must belong to the group that owns the property. (Administrator role overrides
this restriction.)

**Add Property to a Single Channel - CURRENTLY NOT WORKING** 

.../properties/<property_name>/<channel_name>

Method: PUT     Payload: Single Property     Required Role: PropertyMod

Add property with the given property_name to the channel with the given channel_name. An existing
property value is replaced by the payload data.

The authenticated user must belong to the group that owns the property. (Administrator role overrides
this restriction.)

**Add Multiple Properties**

.../properties

Method: PUT    Payload: List of Properties   Required Role: PropertyMod

Add the properties in the payload to the directory. If a payload property contains an embedded
<channels> list, the property is added to all channels in that list. In this case, the value for each property
instance is taken from the property definition inside the channel on the embedded channel list. The
property is set exclusively on all channels in the embedded list, removing it from all channels that are
not included on the list. Existing property values are replaced by the payload data.

For all properties that are to be replaced or added, the authenticated user is required to be a member of
their owner group. (Administrator role overrides this restriction.)

**Add Property to Multiple Channels - CURRENTLY NOT WORKING**

.../properties/<name>

Method: POST     Payload: Single Property    Required Role: PropertyMod

Add property with the given name to all channels in the payload data. If the payload contains an
embedded <channels> list, the property is added to all channels in that list. In this case, the value for
each property instance is taken from the property definition inside the channel in the embedded channel
list. Existing property values are replaced by the payload data. If the payload property name or owner
are different from the current values, the database name/owner are changed.

The authenticated user must belong to the group that owns the property. If the operation changes the
ownership, the user must belong to both the old and the new group. (Administrator role overrides these
restrictions.)

**Add Multiple Properties - CURRENTLY NOT WORKING**

.../properties

Method: POST    Payload: List of Properties  Required Role: PropertyMod

Add the properties in the payload to the directory. If a payload property contains an embedded
<channels> list, the property is added to all channels in that list. In this case, the value for each property
instance is taken from the property definition inside the channel on the embedded channel list. The
property is set exclusively on all channels in the embedded list, removing it from all channels that are
not included on the list. Existing property values are replaced by the payload data.

For all properties that are to be replaced or added, the authenticated user is required to be a member of
their owner group. (Administrator role overrides this restriction.)

**Remove Property from Single Channel**

**.../properties/<property_name>/<channel_name>**

Method: DELETE						         Required Role: PropertyMod

Remove property with the given property_name from the channel with the given channel_name.

The authenticated user must belong to the group that owns the property to be removed. (Administrator role overrides
this restriction.)

**Remove Property**

**.../properties/<name>**

Method: DELETE						         Required Role: PropertyMod

Remove property with the given name from all channels.

The authenticated user must belong to the group that owns the property. (Administrator role overrides
this restriction.)

Tag Resources
-----------------

**Retrieve a Tag** 

**.../tags/<name>**

Method: GET		Returns: Single Tag		     Required Role: None

Return the tag with the given name, listing all tagged channels in an embedded <channels> structure.

**List Tags**

**.../tags**

Method: GET    Returns: List of Tags         Required Role: None

Return the list of all tags in the directory.

**Add Tag to Single Channel - CURRENTLY NOT WORKING**

.../tags/<tag_name>/<channel_name>

Method: PUT     Payload: Single Tag          Required Role: TagMod

Add tag with the given tag_name to the channel with the given channel_name.

The authenticated user must belong to the group that owns the tag. (Administrator role overrides this
restriction.)

**Create/Update a Tag**

.../tags/<name>

Method: PUT     Payload: Single Tag          Required Role: TagMod

Create or completely replace the existing tag name with the payload data. If the payload contains an
embedded <channels> list, the tag is added to all channels in that list. The tag is set exclusively on all
channels in the payload data, removing it from all channels that are not included in the payload.

The authenticated user must belong to the group that owns the tag. (Administrator role overrides this
restriction.)

**Add Tag to Multiple Channels**

.../tags/<name>

Method: POST     Payload: Single Tag	     Required Role: TagMod

Add tag with the given name to all channels in the payload data. If the payload contains an embedded
<channels> list, the tag is added to all channels in that list. If the payload tag name or owner are
different from the current values, the database name/owner are changed.

The authenticated user must belong to the group that owns the tag. If the operation changes the
ownership, the user must belong to both the old and the new group. (Administrator role overrides these
restrictions.)

**Add Multiple Tags - CURRENTLY NOT WORKING**

.../tags

Method: POST 	Payload: List of Tags	     Required Role: TagMod

Add the tags in the payload to the directory. If a payload tag contains an embedded <channels> list, the
tag is added to all channels in that list. The tag is set exclusively on all channels in the embedded list,
removing it from all channels that are not included.

For all tags that are to be replaced or added, the authenticated user is required to be a member of their
owner group. (Administrator role overrides this restriction.)

**Delete Tag from Single Channel**

**.../tags/<tag_name>/<channel_name>**

Method: DELETE						         Required Role: TagMod

Remove tag with the given tag_name from the channel with the given channel_name.

The authenticated user must belong to the group that owns the tag to be removed. (Administrator role
overrides this restriction.)

**Delete Tag**

**.../tags/<name>**

Method: DELETE						         Required Role: TagMod

Remove tag with the given name from all channels.

The authenticated user must belong to the group that owns the tag. (Administrator role overrides this
restriction.)