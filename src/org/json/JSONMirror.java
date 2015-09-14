package org.json;


/**
 * This extension of the JSONString interface allows not only
 * to create JSON Strings from an object, but it also allows
 * to re-construct the original object from a given JSON Object.
 */
public interface JSONMirror<Obj> extends JSONString {

	public Obj fromJSONObject(JSONObject o);

}