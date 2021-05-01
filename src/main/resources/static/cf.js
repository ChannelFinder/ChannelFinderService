
"use strict";

var ChannelFinder = (function() {

function ChannelFinder(opts) {
    opts = opts || {};
    this.baseurl = opts.baseurl || "/ChannelFinder";
}

function doReq(suffix, opts) {
    opts = opts || {};
    opts.headers = opts.headers || new Headers();
    opts.cache = "no-cache";
    if(opts.json) {
        opts.body = JSON.stringify(opts.json);
        opts.headers.append("Content-Type", "application/json");
        delete opts.json;
    }
    const req = new Request(this.baseurl + suffix, opts);
    return fetch(req)
    .then(resp => {
        if(!resp.ok) {
            if(resp.headers.get("Content-Type").startsWith("application/json")) {
                return resp.json().then(json => {
                    throw new Error(json.error+" : "+json.message);
                });
            }
            throw new Error(resp.status+" "+resp.url);
        }
        return resp;
    });
}

/* Return Promise yielding the server info Object
 */
ChannelFinder.prototype.info = function() {
    return doReq.call(this, "").then(resp => resp.json());
};

/* Return a Promise yielding an Array of tags.  [{name:"", owner:""}]
 */
ChannelFinder.prototype.tags = function() {
    return doReq.call(this, "/resources/tags").then(resp => resp.json());
}

/* Return a Promise yielding an Array of properties.  [{name:"", owner:""}]
 */
ChannelFinder.prototype.properties = function() {
    return doReq.call(this, "/resources/properties").then(resp => resp.json());
}

ChannelFinder.prototype.query = function(args) {
    const Q = new URLSearchParams();
    if(args.pattern) {
        Q.append("~name", args.pattern);
    }
    for(const tag of args.tags || []) {
        Q.append("~tag", tag);
    }
    for(const prop in args.properties || {}) {
        Q.append(prop, args.properties[prop]);
    }
    if(args.size) {
        Q.append("~size", args.size);
    }
    if(args.from) {
        Q.append("~from", args.from);
    }
    return doReq.call(this, "/resources/channels?" + Q.toString()).then(resp => resp.json());
}

/* Create a channel, tag, or property.
 * Return a Promise yielding the created channel, tag, or property
 */
ChannelFinder.prototype.create = function(args) {
    if(args.tag) {
        return doReq.call(this, "/resources/tags/" + args.tag, {
            method: "PUT",
            json: {name: args.tag, owner:args.owner},
        });

    } else if(args.property) {
        return doReq.call(this, "/resources/properties/" + args.property, {
            method: "PUT",
            json: {name: args.property, owner:args.owner},
        });

    } else if(args.channels) {
        return doReq.call(this, "/resources/channels", {
            method: "PUT",
            json: args.channels.map(chan => {
                const ent = {name:chan.name, owner:args.owner, tags:[], properties:[]};
                for(const tag of chan.tags) {
                    ent.tags.push({name:tag});
                }
                for(const prop in chan.properties) {
                    ent.properties.push({name:prop, value:chan.properties[prop]});
                }
                return ent;
            }),
        });

    } else {
        throw new Error("missing required argument");
    }
}

/* Delete a channel, tag, or property.
 * Return a Promise yielding nothing.
 */
ChannelFinder.prototype.delete = function(args) {
    if(args.tag) {
        return doReq.call(this, "/resources/tags/" + args.tag, {
            method: "DELETE",
        });

    } else if(args.property) {
        return doReq.call(this, "/resources/properties/" + args.property, {
            method: "DELETE",
        });

    } else if(args.channel) {
        return doReq.call(this, "/resources/channels/" + args.channel, {
            method: "DELETE",
        });

    } else {
        throw new Error("missing required argument");
    }
}

/* Apply a tag or property to a channel or channels */
ChannelFinder.prototype.apply = function(args) {
    const channels = args.channels || [args.channel];
    if(args.tag || args.property) {
        var url;
        const ent = {owner:args.owner};
        if(args.tag) {
            url = "/resources/tags/" + args.tag;
            ent.name = args.tag;

        } else if(args.property) {
            url = "/resources/properties/" + args.property;
            ent.name = args.property;
        }

        ent.channels = channels.map(chan => {
            const ret = {name:chan, owner:args.owner};
            if(args.property) {
                ret.properties = [{name:args.property, value:args.value, owner:args.owner}];
            }
            return ret;
        });

        return doReq.call(this, url, {
            method: "POST",
            json: ent,
        });

    } else {
        throw new Error("missing required argument");
    }
}

/* Remove a tag or property from a channel or channels */
ChannelFinder.prototype.remove = function(args) {
    const channels = args.channels || [args.channel];
    if(args.tag || args.property) {
        return channels.reduce((P, chan) => {
            return P.then(() => {
                var url;
                if(args.tag) {
                    url = "/resources/tags/" + args.tag + "/" + chan;

                } else if(args.property) {
                    url = "/resources/properties/" + args.property + "/" + chan;
                }
                return doReq.call(this, url, {
                    method: "DELETE",
                });
            });
        }, Promise.resolve());

    } else {
        throw new Error("missing required argument");
    }
}

return ChannelFinder;
})();
