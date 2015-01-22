package asclib.sys;

public class Misc {

	public static String FindModule(String name) {
		String classpath = System.getProperty("java.class.path");
		return classpath;
	}

}
