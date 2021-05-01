"use strict";

const CF = new ChannelFinder();

function updateInfo() {
    const elem = document.getElementById("servinfo");
    CF.info()
    .then(info => {
        elem.innerText = JSON.stringify(info);
    })
    .catch(exc => {
        console.error(exc);
        elem.innerText = exc;
    });
}

function getOwner() {
    return document.getElementById("ownertxt").value;
}

function addChild(parent, elem, args) {
    args = args || {};
    if(args.tip) {
        parent = parent.appendChild(document.createElement("div"));
        parent.classList.add("tooltip");
    }
    const child = parent.appendChild(document.createElement(elem));
    if(args.klass) {
        child.classList.add(args.klass);
    }
    if(args.tip) {
        const tip = parent.appendChild(document.createElement("span"));
        tip.classList.add("tooltext");
        tip.innerText = args.tip;
    }
    return child;
}

function doQuery() {
    const query = document.getElementById("querytxt").value;
    // clear previous results
    const results = document.getElementById("queryresult");
    while(results.firstChild) {
        results.removeChild(results.firstChild);
    }

    const parts = query.split(" ").map(part => part.trim()).filter(part => part.length>0);
    if(parts.length==0) {
        return;
    }
    const Q = {pattern:parts.shift(), tags:[], properties:{}, size:1024};
    for(const part of parts) {
        const [key, val] = part.split("=", 2);
        if(val===undefined) {
            Q.tags.push(key);
        } else {
            Q.properties[key] = val;
        }
    }

    CF.query(Q)
    .then(channels => {
        results.replaceChildren(...channels.map(ent => {
            const li = document.createElement("li");

            addChild(li, "span", {klass:"cf-chan", tip:"owner "+ent.owner}).innerText = ent.name;

            li.append("  ");

            for(const tag of ent.tags || []) {
                addChild(li, "span", {klass:"cf-tagprop", tip:"owner "+tag.owner}).innerText = tag.name;
                li.append(" ");
            }

            for(const prop of ent.properties || []) {
                addChild(li, "span", {klass:"cf-tagprop", tip:"owner "+prop.owner}).innerText = prop.name;
                li.append("=");
                addChild(li, "span", {klass:"cf-value"}).innerText = prop.value;
                li.append(" ");
            }

            return li;
        }));
    })
    .catch(exc => {
        console.error(exc);
        const li = addChild(results, "li");
        addChild(li, "span", {klass:"cf-error"}).innerText = exc.message;
    });
}

function createChannels() {
    const err = document.getElementById("chanerr");
    const channels = document.getElementById("chanlist").value
    .split("\n")
    .map(line => line.trim())
    .filter(line => line.length>0)
    .map(line => {
        const parts = line.split(" ");
        const ent = {name:parts.shift(), tags:[], properties:{}};
        for(const part of parts) {
            const [key, val] = part.split("=", 2);
            if(val===undefined) {
                ent.tags.push(key);
            } else {
                ent.properties[key] = val;
            }
        }
        return ent;
    });

    if(channels.length) {
        CF.create({channels:channels, owner:getOwner()})
        .then(() => {
            err.innerText = "";
        })
        .catch(exc => {
            err.innerText = exc.message;
        });
    }
}

function deleteChannels() {
    const err = document.getElementById("chanerr");
    // delete channels individually, and in sequence (to avoid thundering hurd)
    document.getElementById("chanlist").value
    .split("\n")
    .reduce((P, chan) => {
        chan = chan.trim();
        if(chan.length) {
            P = P.then(() => CF.delete({channel:chan}));
        }
        return P;
    }, Promise.resolve())
    .then(() => {
        err.innerText = "";
    })
    .catch(exc => {
        err.innerText = exc.message;
    });
}

function tagChannels() {
    const err = document.getElementById("chanerr");
    const [name, val] = document.getElementById("chantagprop").value.split("=",2);
    const args = {
        owner:getOwner(),
        channels:document.getElementById("chanlist").value
        .split("\n")
        .map(line => line.trim())
        .filter(line => line.length>0)
    };

    if(name && args.channels.length>0) {
        if(val) {
            args.property = name;
            args.value = val;
        } else {
            args.tag = name;
        }
    }

    CF.apply(args)
    .then(() => {
        err.innerText = "";
    })
    .catch(exc => {
        err.innerText = exc.message;
    });
}

function untagChannels() {
    const err = document.getElementById("chanerr");
    const [name, val] = document.getElementById("chantagprop").value.split("=",2);
    const args = {
        owner:getOwner(),
        channels:document.getElementById("chanlist").value
        .split("\n")
        .map(line => line.trim())
        .filter(line => line.length>0)
    };

    if(name && args.channels.length>0) {
        if(val) {
            args.property = name;
            args.value = val;
        } else {
            args.tag = name;
        }
    }

    CF.remove(args)
    .then(() => {
        err.innerText = "";
    })
    .catch(exc => {
        err.innerText = exc.message;
    });
}

function translateList(tp, selector, list) {
    document.querySelector(selector).replaceChildren(...list.map(ent => { // ent: {name:"", owner:""}
        const li = document.createElement("li");

        addChild(li, "span", {klass:"cf-tagprop", tip:"owner "+ent.owner}).innerText = ent.name;

        li.append(" ... ");
        const C = addChild(li, "button");
        C.type = "button";
        C.innerText = "Delete";
        C.onclick = function() {
            console.log("Delete", tp, ent.name);
            var args = {};
            args[tp] = ent.name;
            CF.delete(args)
            .finally(() => {
                updateTagProp();
            });
        };
        return li;
    }));
}

function updateTagProp() {
    CF.tags().then(list => translateList("tag", "#taglist", list));
    CF.properties().then(list => translateList("property", "#proplist", list));
}

function addTagProp(tp) {
    const err = document.getElementById("tagproperr");
    const name = document.querySelector("#tagproptxt").value;
    const args = {owner:getOwner()};
    args[tp] = name;
    console.log("Create", tp, name);
    CF.create(args)
    .then(() => {
        console.log("Created", tp, name);
        err.innerText = "";
    })
    .catch(exc => {
        err.innerText = exc.message;
    })
    .finally(() => {
        updateTagProp();
    })
}

window.onload = function() {
    updateInfo();
    updateTagProp();
}
