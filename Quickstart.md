# Introduction #

I quest the easiest way is to see examples. Check out the sourcecode and rename `GsRestTest.java.bak` to `GsRestTest.java`. Next start your local Geoserver and enter your credentials in `GsRestTest.java`. Now run the tests and look at how they work. It is really simple:

```
 @Test
 public void testSendShape() {

	deleteWorkspace("ws", true);
	createWorkspace("ws");

	URL soilsZipFile = GsRestTest.class.getResource("..../shape.zip");

	uploadShape("ws", "zipUploadTestDatastoreName",
			soilsZipFile);
 }
```