package magicionOZ;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.simple.JSONValue;

/**
 * Thread chargeant les données dispnible sur l'API web afin de les publier à l'application principale
 */
public class OzGetter extends Thread implements OzProvider {

	/**
	 * constante contenant l'url sur laquelle récupérer les données du magicien d'oz
	 */
	private static final String	URL_STR = "http://carretero.ovh/read";

	/**
	 * temps entre deux requête GET sur l'url
	 */
	private static final int 	REFRESH_RATE = 2000;
	
	/**
	 * objet java url de connexion
	 */
	private URL 				url;
	
	/**
	 * conexion http à l'url spécifié
	 */
	private HttpURLConnection 	connection;
	
	/**
	 * class implémentant {@link OzSuscriber} qui recevra les données lors de leur mise à jour
	 */
	private OzSuscriber 		suscriber;

	/**
	 * Default constructor
	 * initialise les variable locales afin de récupérer les données server
	 */
	public OzGetter() {
		try {
			this.url = new URL(URL_STR);
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
		}

		try {
			this.connection = (HttpURLConnection) this.url.openConnection();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	/**
	 * @return une string contenant les données du serveur (écrite par le magicien d'oz)
	 * @throws IOException
	 */
	private String fetchData() throws IOException {
		if (this.connection.getResponseCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "+ this.connection.getResponseCode());
		}

		BufferedReader br = new BufferedReader(
				new InputStreamReader(
						(this.connection.getInputStream()))
				);

		StringBuffer output = new StringBuffer();
		String nline = null;
		while ((nline = br.readLine()) != null) {
			output.append(nline);
		}
		return output.toString();
	}

	/**
	 * récupère à intervalle régulier les données du serveur et publie ces informations au suscriber
	 */
	@Override
	public void run() {
		this.setPriority(MIN_PRIORITY);
		String previousData = "";
		while(!isInterrupted()) {
			String newData = "";
			try {
				newData = fetchData();
			} catch (IOException e) {
				System.out.println(e.getMessage());
			}
			if(!previousData.equals(newData) && this.suscriber != null) {
				publish(newData);
				previousData = newData;
			}
			syncWait();
		}
		this.connection.disconnect();
	}
	
	synchronized void syncWait() {
		try {
			this.wait(REFRESH_RATE);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * parse le json et l'envoi au suscriber
	 * @param json le json conteant les donnée
	 */
	private void publish(String json) {
		String res = (String) JSONValue.parse(json);
		if(res != null) {
			this.suscriber.publish(res.trim());
		}
	}

	@Override
	public void suscribe(OzSuscriber os) {
		this.suscriber = os;
	}
}
