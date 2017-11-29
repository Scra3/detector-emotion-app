package magicionOZ;

/**
 * Interface implémenter par le service de récupération des données du magicien 
 * d'Oz permettant à une classe (unique) de s'ajouter en tant que listenner
 */
public interface OzProvider {
	
	/**
	 * permet à un OzSuscriber de s'ajouter en tant que listenner
	 * @param os un OzSuscriber qui recevra l'information
	 */
	public void suscribe(OzSuscriber os);
}
