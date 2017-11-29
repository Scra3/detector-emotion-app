package magicionOZ;

public class main implements OzSuscriber{
	
	public static void main(String[] args) {
		OzGetter o = new OzGetter();
		o.suscribe(new main());
		o.start();
	}

	@Override
	public void publish(String str) {
		System.out.println(str);
	}

}
