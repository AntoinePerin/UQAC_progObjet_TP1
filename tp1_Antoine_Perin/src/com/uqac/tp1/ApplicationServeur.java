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
	 * prend le num?ro de port, cr?e un SocketServer sur le port
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
	 * ce qui est envoy? ? travers la Socket, recr?e l?objet Commande envoy? par le
	 * client, et appellera traiterCommande(Commande uneCommande)
	 */
	public void aVosOrdres(String fichTrace) {

		// Fichier trace serveur
		PrintWriter traceWriter = null;
		try {
			PrintWriter writer = new PrintWriter(fichTrace);
			traceWriter = new PrintWriter(writer, true);
			traceWriter.println("Server en ?coute sur le port " + this.ss.getLocalPort() + "\n");

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
				traceWriter.println("Client " + client.getRemoteSocketAddress().toString() + " connect?");

				// creation des streams
				ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
				ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());

				// Lire objet et recreer Commande
				Commande cmd = (Commande) ois.readObject();

				// Appel de traiter commande et envoie du r?sultat au client
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
	 * traiterCommande : prend uneCommande dument formatt?e, et la traite. D?pendant du type de
	 * commande, elle appelle la m?thode sp?cialis?e. Renvoie le resultat de la commande
	 */
	public Object traiterCommande(Commande uneCommande, PrintWriter traceWriter) {

		Object resultatTraitement = new Object();
		resultatTraitement=null;
		
		// Ecriture commande ? traiter fichier trace serveur
		traceWriter.println("Traitement de la commande : " + uneCommande);

		/// On r?cup?re les propri?t?s de la commande dans un tableau
		String[] proprietesCommande = uneCommande.getProprietes();

		// On r?alise un switch sur la propri?t? 0 correspondant a l'id de la cmd
		switch (proprietesCommande[0]) {

		case "compilation":
			String[] cheminsFichierACompiler = proprietesCommande[1].split(",");
			for (int i = 0; i < cheminsFichierACompiler.length; i++) {
				traiterCompilation(cheminsFichierACompiler[i], proprietesCommande[2]);
			}
			resultatTraitement="La Compilation de(s) ce(s) fichier(s) a bien ?t? r?alis?";
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

			// Si la fonction a des attributs on les isoles dans des tableaux s?parement
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
	 * traiterLecture : traite la lecture d?un attribut. Renvoies le r?sultat par le
	 * socket
	 */
	public Object traiterLecture(Object pointeurObjet, String attribut) {
		try {
			//traite la lecture d?un attribut
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
	 * traiterEcriture : traite l??criture d?un attribut. Confirmes au client que
	 * l??criture s?est faite correctement.
	 */
	public String traiterEcriture(Object pointeurObjet, String attribut, Object valeur) {
		try {
			Class classeDeLobjet = pointeurObjet.getClass();
			Method mSetAttribut = classeDeLobjet.getMethod("set" + capitalize(attribut), String.class);
			mSetAttribut.invoke(pointeurObjet, valeur);
			return "L'?criture de l'attribut a ?t? effectu?";

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
	 * traiterCreation : traite la cr?ation d?un objet. Confirme au client que la
	 * cr?ation s?est faite correctement.
	 */
	public String traiterCreation(Class classeDeLobjet, String identificateur) {

		try {
			// traiter la cr?ation d'un objet
			Constructor ct = classeDeLobjet.getConstructor();
			Object o = ct.newInstance();
			listeObjet.put(identificateur, o);
			return "La nouvelle instance de la classe a bien ?t? cr??e";

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
	 * traiterChargement : traite le chargement d?une classe. Confirmes au client
	 * que la cr?ation s?est faite correctement.
	 */
	public String traiterChargement(String nomQualifie) {

		try {
			Class c = Class.forName(nomQualifie);
			ClassLoader classLoader = c.getClassLoader();
			return "Le chargement du fichier a ?t? effectu?";

			// TODO Envoyer confirmation client que le chargement des fichiers "" et ""
			// s'est bien pass?

		} catch (ClassNotFoundException e) {
			return e.toString();
		}
		

	}

	/**
	 * traiterCompilation : traite la compilation d?un fichier source java. Confirme
	 * au client que la compilation s?est faite correctement. Le fichier source est
	 * donn? par son chemin relatif par rapport au chemin des fichiers sources.
	 */
	public void traiterCompilation(String cheminRelatifFichierSource, String cheminRelatifFichierClass) {

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		compiler.run(null, null, null, cheminRelatifFichierSource);

		// Pour sp?cifier un chemin de destination des fichiers class --> pas eu le temps
		// compiler.run(null, null, null, "-d",cheminRelatifFichierClass,
		// cheminRelatifFichierSource);
	}

	/**
	 * traiterAppel : traite l?appel d?une m?thode, en prenant comme argument
	 * l?objet sur lequel on effectue l?appel, le nom de la fonction ? appeler, un
	 * tableau de nom de types des arguments, et un tableau d?arguments pour la
	 * fonction. Le r?sultat de la fonction est renvoy? par le serveur au client (ou
	 * le message que tout s?est bien pass?)
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
	 * programme principal. Prend 4 arguments: 1) num?ro de port, 2) r?pertoire
	 * source, 3) r?pertoire classes, et 4) nom du fichier de traces (sortie) Cette
	 * m?thode doit cr?er une instance de la classe ApplicationServeur,
	 * l?initialiser puis appeler aVosOrdres sur cet objet
	 */
	public static void main(String[] args) {

		ApplicationServeur serveur = new ApplicationServeur(Integer.valueOf(args[0]));
		serveur.aVosOrdres(args[3]);

	}

	/**
	 * capitalize : permet de mettre la premi?re lettre d'une String en majuscule
	 * 
	 */

	public static String capitalize(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

}
