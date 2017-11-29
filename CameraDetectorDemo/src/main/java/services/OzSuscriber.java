package services;

/**
 * 
 */
public interface OzSuscriber {
	
	/**
	 * should be synchronized
	 * @param str a string from the interweb
	 */
	public void publish(String str);
	
}
