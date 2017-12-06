package services;

import android.util.Log;
import android.widget.EditText;

import com.affectiva.cameradetectordemo.MainActivity;
import com.affectiva.cameradetectordemo.R;

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
public class OzGetter implements Runnable {

    /**
     * constante contenant l'url sur laquelle récupérer les données du magicien d'oz
     */
    private static final String URL_STR = "http://carretero.ovh/read";
    private final MainActivity m;

    /**
     * temps entre deux requête GET sur l'url
     */
    private int refresh_rate = 1000;

    /**
     * objet java url de connexion
     */
    private URL url;
    private String res = "";

    /**
     * Default constructor
     * initialise les variable locales afin de récupérer les données server
     */
    public OzGetter(MainActivity m) {
        this.m = m;
        try {
            this.url = new URL(URL_STR);
        } catch (MalformedURLException e) {
            System.out.println(e.getMessage());
        }
    }

    synchronized public void start(){
        Log.i("start", "thread started");
        this.refresh_rate = 1000;
        this.notifyAll();
    }

    /**
     * @return une string contenant les données du serveur (écrite par le magicien d'oz)
     * @throws IOException
     */
    private String fetchData() throws IOException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) this.url.openConnection();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        if (connection == null) {
            throw new RuntimeException("Failed : connection : ");
        }
        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Failed : HTTP error code : " + connection.getResponseCode());
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        (connection.getInputStream()))
        );

        StringBuffer output = new StringBuffer();
        String nline = null;
        while ((nline = br.readLine()) != null) {
            output.append(nline);
        }
        connection.disconnect();
        return output.toString();
    }

    /**
     * récupère à intervalle régulier les données du serveur et publie ces informations au suscriber
     */
    @Override
    public void run() {
        Thread currentThread = Thread.currentThread();
        currentThread.setPriority(Thread.MIN_PRIORITY);

        String previousData = "";
        while (!currentThread.isInterrupted()) {
            Log.i("run", "running");
            String newData = "";
            try {
                newData = fetchData();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            if (!previousData.equals(newData)) {
                publish(newData);
                previousData = newData;
            }
            syncWait();
        }
    }

    synchronized void syncWait() {
        try {
            this.wait(refresh_rate);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * parse le json et l'envoi au suscriber
     *
     * @param json le json conteant les donnée
     */
    private void publish(String json) {
        String res = (String) JSONValue.parse(json);
        this.res = res.trim();
        m.display(res);
    }

    public String getRes(){
        return res;
    }
}