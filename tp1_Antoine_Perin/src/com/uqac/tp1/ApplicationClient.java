package com.uqac.tp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;

public class ApplicationClient {

	private String hostname;
	private int port;

	// Constructeur
	public ApplicationClient(String hostname, int port) {

		this.hostname = hostname;
		this.port = port;

	}

	public ApplicationClient() {
	}

	/**
	 * prend le fichier contenant la liste des commandes, et le charge dans une
	 * variable du type Commande qui est retourn�e
	 */
	public Commande saisisCommande(BufferedReader br) {

		try {

			String ligne = br.readLine();// lire la ligne du fichier

			if (ligne == null) {// Si rien sur la ligne du fichier txt alors on retourne null
				return null;
			} else {
				final String SEPARATEUR = "#";// S�parer la ligne de commande lu avec le separateur #
				String[] ligneSeparee = ligne.split(SEPARATEUR);
				Commande cmd = new Commande(ligneSeparee);
				return cmd;
			}

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * initialise : ouvre les diff�rents fichiers de lecture et �criture
	 */
	public void initialise(String fichCommandes, String fichSortie) {

		// Ouverture en lecture

	}

	/**
	 * prend une Commande d�ment format�e, et la fait ex�cuter par le serveur. Le
	 * r�sultat de l�ex�cution est retourn�e. Si la commande ne retourne pas de
	 * r�sultat, on retourne null. Chaque appel doit ouvrir une connexion, ex�cuter,
	 * et fermer la connexion. Si vous le souhaitez, vous pourriez �crire six
	 * fonctions sp�cialis�es, une par type de commande d�crit plus haut, qui seront
	 * appel�es par traiteCommande(Commande uneCommande)
	 */

	public Object traiteCommande(Commande uneCommande) {
		
		Object resultatTraitement = new Object();
		
		try {
			// Creation d'un nouveau socket
			Socket s = new Socket(this.hostname, this.port);
			
			// creations des streams
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(s.getInputStream());

			// Ecrire objet
			oos.writeObject(uneCommande);
			
			//Recuperer r�sultat traitement
			resultatTraitement = ois.readObject();

			// fermer Stream
			oos.flush();
			oos.close();
			ois.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Le r�sultat de l�ex�cution est retourn�e.
		return resultatTraitement;

	}

	/**
	 * cette m�thode vous sera fournie plus tard. Elle indiquera la s�quence
	 * d��tapes � ex�cuter pour le test. Elle fera des appels successifs �
	 * saisisCommande(BufferedReader fichier) et traiteCommande(Commande
	 * uneCommande).
	 * 
	 */
	public void scenario(String fichCommandes, String fichSortie) {

		try {
			// Lecture fichiher local commande
			File file = new File(fichCommandes);
			FileReader fr = new FileReader(file.getAbsoluteFile());
			BufferedReader commandesReader = new BufferedReader(fr);

			// Ecriture fichier local sortie
			PrintWriter sortieWriter = new PrintWriter(fichSortie);

			// Scenario
			sortieWriter.println("Debut des traitements:");
			Commande prochaine = saisisCommande(commandesReader);

			while (prochaine != null) {
				sortieWriter.println("\tTraitement de la commande " + prochaine + " ...");
				Object resultat = traiteCommande(prochaine);
				sortieWriter.println("\t\tResultat: " + resultat);
				prochaine = saisisCommande(commandesReader);
			}

			sortieWriter.println("Fin des traitements");

			// On ferme le reader et le writer
			commandesReader.close();
			sortieWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * programme principal. Prend 4 arguments: 1) �hostname� du serveur, 2) num�ro
	 * de port, 3) nom fichier commandes, et 4) nom fichier sortie. Cette m�thode
	 * doit cr�er une instance de la classe ApplicationClient, l�initialiser, puis
	 * ex�cuter le sc�nario
	 */
	public static void main(String[] args) {
		ApplicationClient client = new ApplicationClient(args[0], Integer.valueOf(args[1]));
		// client.initialise(args[2], args[3]);
		// ApplicationClient client = new ApplicationClient();
		client.scenario(args[2], args[3]);
	}

}
