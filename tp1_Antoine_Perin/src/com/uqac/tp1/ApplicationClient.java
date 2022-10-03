package com.uqac.tp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
	 * variable du type Commande qui est retournée
	 */
	public Commande saisisCommande(BufferedReader br) {

		try {

			String ligne = br.readLine();// lire la ligne du fichier

			if (ligne == null) {// Si rien sur la ligne du fichier txt alors on retourne null
				return null;
			} else {
				final String SEPARATEUR = "#";// Séparer la ligne de commande lu avec le separateur #
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
	 * initialise : ouvre les différents fichiers de lecture et écriture
	 */
	public void initialise(String fichCommandes, String fichSortie) {

		// Ouverture en lecture

	}

	/**
	 * prend une Commande dûment formatée, et la fait exécuter par le serveur. Le
	 * résultat de l’exécution est retournée. Si la commande ne retourne pas de
	 * résultat, on retourne null. Chaque appel doit ouvrir une connexion, exécuter,
	 * et fermer la connexion. Si vous le souhaitez, vous pourriez écrire six
	 * fonctions spécialisées, une par type de commande décrit plus haut, qui seront
	 * appelées par traiteCommande(Commande uneCommande)
	 */

	public Object traiteCommande(Commande uneCommande) {

		try {
			// Creation d'un nouveau socket
			Socket s = new Socket(this.hostname, this.port);
			// Envoyer objet au serveur
			ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());

			// Ecrire objet
			oos.writeObject(uneCommande);

			// fermer Stream
			oos.flush();
			oos.close();

			// Résultat de l'execution est retourné

			// ObjectInputStream ois = new ObjectInputStream(this.s.getInputStream());

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Le résultat de l’exécution est retournée.

		return uneCommande;

	}

	/**
	 * cette méthode vous sera fournie plus tard. Elle indiquera la séquence
	 * d’étapes à exécuter pour le test. Elle fera des appels successifs à
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

			// Commande prochaine = saisisCommande(commandesReader);
			// Object resultat = traiteCommande(prochaine);

			// Ecriture fichier local sortie
			PrintWriter sortieWriter = new PrintWriter(fichSortie);

			// Scenario
			sortieWriter.println("Debut des traitements:");
			Commande prochaine = saisisCommande(commandesReader);

			while (prochaine != null) {
				System.out.println(prochaine);
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
	 * programme principal. Prend 4 arguments: 1) “hostname” du serveur, 2) numéro
	 * de port, 3) nom fichier commandes, et 4) nom fichier sortie. Cette méthode
	 * doit créer une instance de la classe ApplicationClient, l’initialiser, puis
	 * exécuter le scénario
	 */
	public static void main(String[] args) {
		ApplicationClient client = new ApplicationClient(args[0], Integer.valueOf(args[1]));
		// client.initialise(args[2], args[3]);
		// ApplicationClient client = new ApplicationClient();
		client.scenario(args[2], args[3]);
	}

}
