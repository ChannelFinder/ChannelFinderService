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
Method: GET    Returns: List of Channels     Required Role: None

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

Method: POST	Payload: List of Channels		Required Role: ChannelMod

Add the channels in the payload to the directory. Existing channels are replaced by the payload data. For all channels that are to be replaced or added, the authenticated user is required to be a member of their owner group. (Administrator role overrides this restriction.)

**Delete a Channel**

**.../channels/<name>**

Method: DELETE						Required Role: ChannelMod

Delete the existing channel name and all its properties and tags.

The authenticated user must be a member of the group that owns the channel to be deleted. (Administrator role overrides this restriction.)

