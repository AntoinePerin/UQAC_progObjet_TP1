package com.uqac.tp1;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.ClassLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.Class;

public class ApplicationServeur {

	private ServerSocket ss;
	private Map<String, Object> listeObjet;

	/**
	 * prend le numéro de port, crée un SocketServer sur le port
	 */
	public ApplicationServeur(int port) {
		try {
			this.ss = new ServerSocket(port);
			this.listeObjet = new HashMap<String, Object>();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Se met en attente de connexions des clients. Suite aux connexions, elle lit
	 * ce qui est envoyé à travers la Socket, recrée l’objet Commande envoyé par le
	 * client, et appellera traiterCommande(Commande uneCommande)
	 */
	public void aVosOrdres(String fichTrace) {

		// Fichier trace serveur
		PrintWriter traceWriter = null;
		try {
			PrintWriter writer = new PrintWriter(fichTrace);
			traceWriter = new PrintWriter(writer, true);
			traceWriter.println("Server en écoute sur le port " + this.ss.getLocalPort() + "\n");

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Boucle serveur
		
		while (true) {
			try {
				// Se met en attente de connexions clients
				traceWriter.println("\nEn Attente de connexions");
				Socket client = ss.accept();
				traceWriter.println("Client " + client.getRemoteSocketAddress().toString() + " connecté");

				// creation des streams
				ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
				ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());

				// Lire objet et recreer Commande
				Commande cmd = (Commande) ois.readObject();

				// Appel de traiter commande et envoie du résultat au client
				oos.writeObject(traiterCommande(cmd, traceWriter));
				
				// Fermer stream
				oos.flush();
				oos.close();
				ois.close();
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * traiterCommande : prend uneCommande dument formattée, et la traite. Dépendant du type de
	 * commande, elle appelle la méthode spécialisée. Renvoie le resultat de la commande
	 */
	public Object traiterCommande(Commande uneCommande, PrintWriter traceWriter) {

		Object resultatTraitement = new Object();
		resultatTraitement=null;
		
		// Ecriture commande à traiter fichier trace serveur
		traceWriter.println("Traitement de la commande : " + uneCommande);

		/// On récupère les propriétés de la commande dans un tableau
		String[] proprietesCommande = uneCommande.getProprietes();

		// On réalise un switch sur la propriété 0 correspondant a l'id de la cmd
		switch (proprietesCommande[0]) {

		case "compilation":
			String[] cheminsFichierACompiler = proprietesCommande[1].split(",");
			for (int i = 0; i < cheminsFichierACompiler.length; i++) {
				traiterCompilation(cheminsFichierACompiler[i], proprietesCommande[2]);
			}
			resultatTraitement="La Compilation de(s) ce(s) fichier(s) a bien été réalisé";
			break;

		case "chargement":
			String nomQualifie = proprietesCommande[1];
			resultatTraitement = traiterChargement(nomQualifie);
			break;

		case "creation":
			try {
				Class classeDeLObjet = Class.forName(proprietesCommande[1]);
				String identificateur = proprietesCommande[2];
				resultatTraitement = traiterCreation(classeDeLObjet, identificateur);
			} catch (ClassNotFoundException e) {
				resultatTraitement = e.toString();
			}
			break;

		case "lecture":
			String identificateur = proprietesCommande[1];
			Object pointeurObjet = listeObjet.get(identificateur);
			String attribut = proprietesCommande[2];
			resultatTraitement=traiterLecture(pointeurObjet, attribut);
			break;

		case "ecriture":
			String identificateur2 = proprietesCommande[1];
			Object pointeurObjet2 = listeObjet.get(identificateur2);
			String attribut2 = proprietesCommande[2];
			Object valeur = proprietesCommande[3];
			resultatTraitement = traiterEcriture(pointeurObjet2, attribut2, valeur);
			break;

		case "fonction":
			String identificateur3 = proprietesCommande[1];
			Object pointeurObjet3 = listeObjet.get(identificateur3);
			String nomFonction = proprietesCommande[2];

			String[] types = null;
			Object[] valeurs = null;

			// Si la fonction a des attributs on les isoles dans des tableaux séparement
			// leurs valeurs et leurs types
			if (proprietesCommande.length > 3) {
				String[] listAttribut = proprietesCommande[3].split(",");
				types = new String[listAttribut.length];
				valeurs = new String[listAttribut.length];

				for (int j = 0; j < listAttribut.length; j++) {
					String[] tabIntermediaire = listAttribut[j].split(":");
					types[j] = tabIntermediaire[0];
					valeurs[j] = tabIntermediaire[1];
				}
			}

			resultatTraitement = traiterAppel(pointeurObjet3, nomFonction, types, valeurs);
			break;
		}
		
		//On ecrit le resultat du traitement dans le fichier trace du serveur
		traceWriter.println("Resultat du traitement  : " + resultatTraitement);
		
		//On retourne le resultat du traitement
		return resultatTraitement;

	}

	/**
	 * traiterLecture : traite la lecture d’un attribut. Renvoies le résultat par le
	 * socket
	 */
	public Object traiterLecture(Object pointeurObjet, String attribut) {
		try {
			//traite la lecture d’un attribut
			Class classeDeLobjet = pointeurObjet.getClass();
			Method[] methods = classeDeLobjet.getMethods();
			Method mGetAttribut;
			mGetAttribut = classeDeLobjet.getMethod("get" + capitalize(attribut), null);
			return mGetAttribut.invoke(pointeurObjet);

		} catch (NoSuchMethodException e) {
			return e.toString();
		} catch (SecurityException e) {
			return e.toString();
		} catch (IllegalAccessException e) {
			return e.toString();
		} catch (IllegalArgumentException e) {
			return e.toString();
		} catch (InvocationTargetException e) {
			return e.toString();
		}

	}

	/**
	 * traiterEcriture : traite l’écriture d’un attribut. Confirmes au client que
	 * l’écriture s’est faite correctement.
	 */
	public String traiterEcriture(Object pointeurObjet, String attribut, Object valeur) {
		try {
			Class classeDeLobjet = pointeurObjet.getClass();
			Method mSetAttribut = classeDeLobjet.getMethod("set" + capitalize(attribut), String.class);
			mSetAttribut.invoke(pointeurObjet, valeur);
			return "L'écriture de l'attribut a été effectué";

		} catch (SecurityException e) {
			return e.toString();
		} catch (IllegalArgumentException e) {
			return e.toString();
		} catch (NoSuchMethodException e) {
			return e.toString();
		} catch (IllegalAccessException e) {
			return e.toString();
		} catch (InvocationTargetException e) {
			return e.toString();
		}
	}

	/**
	 * traiterCreation : traite la création d’un objet. Confirme au client que la
	 * création s’est faite correctement.
	 */
	public String traiterCreation(Class classeDeLobjet, String identificateur) {

		try {
			// traiter la création d'un objet
			Constructor ct = classeDeLobjet.getConstructor();
			Object o = ct.newInstance();
			listeObjet.put(identificateur, o);
			return "La nouvelle instance de la classe a bien été créée";

		} catch (InstantiationException e) {
			return e.toString();
		} catch (IllegalAccessException e) {
			return e.toString();
		} catch (NoSuchMethodException e) {
			return e.toString();
		} catch (SecurityException e) {
			return e.toString();
		} catch (IllegalArgumentException e) {
			return e.toString();
		} catch (InvocationTargetException e) {
			return e.toString();
		}

	}

	/**
	 * traiterChargement : traite le chargement d’une classe. Confirmes au client
	 * que la création s’est faite correctement.
	 */
	public String traiterChargement(String nomQualifie) {

		try {
			Class c = Class.forName(nomQualifie);
			ClassLoader classLoader = c.getClassLoader();
			return "Le chargement du fichier a été effectué";

			// TODO Envoyer confirmation client que le chargement des fichiers "" et ""
			// s'est bien passé

		} catch (ClassNotFoundException e) {
			return e.toString();
		}
		

	}

	/**
	 * traiterCompilation : traite la compilation d’un fichier source java. Confirme
	 * au client que la compilation s’est faite correctement. Le fichier source est
	 * donné par son chemin relatif par rapport au chemin des fichiers sources.
	 */
	public void traiterCompilation(String cheminRelatifFichierSource, String cheminRelatifFichierClass) {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		compiler.run(null, null, null, cheminRelatifFichierSource);

		// Pour spécifier un chemin de destination des fichiers class --> pas eu le temps
		// compiler.run(null, null, null, "-d",cheminRelatifFichierClass,
		// cheminRelatifFichierSource);
	}

	/**
	 * traiterAppel : traite l’appel d’une méthode, en prenant comme argument
	 * l’objet sur lequel on effectue l’appel, le nom de la fonction à appeler, un
	 * tableau de nom de types des arguments, et un tableau d’arguments pour la
	 * fonction. Le résultat de la fonction est renvoyé par le serveur au client (ou
	 * le message que tout s’est bien passé)
	 */
	public Object traiterAppel(Object pointeurObjet, String nomFonction, Object[] types, Object[] valeurs) {

		Class classeDeLobjet = pointeurObjet.getClass();

		Class[] parameterType = null;
		if (types != null) {
			parameterType = new Class[types.length];

			// boucle pour parameter type
			for (int i = 0; i < types.length; i++) {
				if (types[i].equals("float")) {
					parameterType[i] = float.class;
				} else if (types[i].equals("ca.uqac.registraire.Etudiant")) {
					parameterType[i] = ca.uqac.registraire.Etudiant.class;
				} else if (types[i].equals("ca.uqac.registraire.Cours")) {
					parameterType[i] = ca.uqac.registraire.Cours.class;
				}
			}
		}

		// boucle pour valeurs
		Object[] valeurs2 = null;
		if (valeurs != null) {
			valeurs2 = new Object[valeurs.length];

			for (int j = 0; j < valeurs.length; j++) {
				if (((String) valeurs[j]).substring(0, 2).equals("ID")) {
					String identifiant = ((String) valeurs[j]).substring(3, ((String) valeurs[j]).length() - 1);
					valeurs2[j] = listeObjet.get(identifiant);
				} else {
					float f = Float.parseFloat((String) valeurs[j]);
					valeurs2[j] = f;
				}
			}
		}

		try {
			Method mFonction = classeDeLobjet.getMethod(nomFonction, parameterType);
			return mFonction.invoke(pointeurObjet, valeurs2);
			
		} catch (IllegalArgumentException e) {
			return e.toString();
		} catch (IllegalAccessException e) {
			return e.toString();
		} catch (InvocationTargetException e) {
			return e.toString();
		} catch (NoSuchMethodException e) {
			return e.toString();
		} catch (SecurityException e) {
			return e.toString();
		}
	}

	/**
	 * programme principal. Prend 4 arguments: 1) numéro de port, 2) répertoire
	 * source, 3) répertoire classes, et 4) nom du fichier de traces (sortie) Cette
	 * méthode doit créer une instance de la classe ApplicationServeur,
	 * l’initialiser puis appeler aVosOrdres sur cet objet
	 */
	public static void main(String[] args) {

		ApplicationServeur serveur = new ApplicationServeur(Integer.valueOf(args[0]));
		serveur.aVosOrdres(args[3]);

	}

	/**
	 * capitalize : permet de mettre la première lettre d'une String en majuscule
	 * 
	 */

	public static String capitalize(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

}
