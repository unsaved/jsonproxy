
/* No interpreter line because this script wouldn't work by direct script
 * invocaton unless you have CLASSPATH set to include the jsonproxy classes
 * (or jar file).
 * For my purposes, from UNIX, use co-located script "driver.bash". */

final com.admc.jsonproxy.Service service = new com.admc.jsonproxy.Service()
//println service.instantiate('one', 'java.lang.String', 'input str')
println service.staticCall("java.lang.String", "format", "(%s) (%d)", ["werd", 345] as Object[]);
println service.size()
println "Size: ${service.size()}"
