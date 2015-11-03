A Java Client to the Geoserver 2.0.2+ RESTful configuration extension. Using this library, one can access a remote Geoserver via HTTP and configure it.

The REST extension of Geoserver has to be installed on Geoserver.

This project uses Java 1.5 and has no extra dependencies!

That is a difference to the _rest_ module found in Geoserver's community-folder (http://svn.codehaus.org/geoserver/branches/2.0.x/src/community/rest/pom.xml), which has dependencies to many Geoserver + Geotools JAR.

Note: 2011/05 Geosolutions release a very similar project: http://geo-solutions.blogspot.com/2011/05/developers-corner-geoserver-manager.html
Since they are Geoserver-core developers, you might want to evaluate that project as well.

The REST API is not fully implemented. Here is a selection of what's there:

```
 * String uploadShape(String workspace, String dsName, URL zip)
 * createDatastoreShapefile(String workspace, String dsName, String dsNamespace, String  relpath, String charset, Boolean memoryMappedBuffer, Boolean createSpatialIndex)
 * String createSld(String stylename, String sldString)
 * boolean createFeatureType(String wsName, String dsName, String ftName)
 * boolean createFeatureType(String wsName, String dsName, String ftName)
 * createDbDatastore(String workspace, String dsName, String dsNamespace, String host, String port, String db, String user, String pwd, String dbType, boolean exposePKs)
 * boolean createWorkspace(String workspaceName)
 * List of String getFeatureTypes(String wsName, String dsName)
 * List of String getLayersUsingDatastore(String wsName, String dsName)
 * List of String getLayerNames()
 * deleteSld(String styleName, Boolean purge)
```