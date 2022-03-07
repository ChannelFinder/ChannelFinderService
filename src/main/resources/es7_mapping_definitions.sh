#!/bin/sh

es_host=localhost
es_port=9200

###
# #%L
# ChannelFinder Directory Service
# %%
# Copyright (C) 2010 - 2016 Helmholtz-Zentrum Berlin für Materialien und Energie GmbH
# %%
# Copyright (C) 2010 - 2012 Brookhaven National Laboratory
# All rights reserved. Use is subject to license terms.
# #L%
###
# The mapping definition for the Indexes associated with the channelfinder v4


#Create the Index
curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/cf_tags?include_type_name=true -d'
{
"mappings":{
  "cf_tag" : {
    "properties" : {
      "name" : {
        "type" : "keyword"
      },
      "owner" : {
        "type" : "keyword"
      }
    }
    }
  }
}'

curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/cf_properties?include_type_name=true -d'
{
"mappings":{
  "cf_property" : {
    "properties" : {
      "name" : {
        "type" : "keyword"
      },
      "owner" : {
        "type" : "keyword"
      }
    }
  }
  }
}'

curl -H 'Content-Type: application/json' -XPUT http://${es_host}:${es_port}/channelfinder?include_type_name=true -d'
{
"mappings":{
  "cf_channel" : {
    "properties" : {
      "name" : {
        "type" : "keyword"
      },
      "owner" : {
        "type" : "keyword"
      },
      "script" : {
        "type" : "keyword"
      },
      "properties" : {
        "type" : "nested",
        "properties" : {
          "name" : {
            "type" : "keyword"
          },
          "owner" : {
            "type" : "keyword"
          },
          "value" : {
            "type" : "keyword"
          }
        }
      },
      "tags" : {
        "type" : "nested",
        "properties" : {
          "name" : {
            "type" : "keyword"
          },
          "owner" : {
            "type" : "keyword"
          }
        }
      }
    }
  }
  }
}'
