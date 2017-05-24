package models;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Classe contenant un message d'un utilisateur envoyé par le serveur.
 * Un message d'un utilisateur est caractérisé par :
 * <ul>
 * 	<li>la date d'arrivée du message</li>
 * 	<li>un auteur</li>
 * 	<li>le contenu du message></li>
 * </ul>
 * Les message peuvent être comparés entre eux pour obtenir l'ordre des messages
 * avec la méthode compareTo(Message m). Les critère d'ordre des messages
 * peuvent être customizés.
 * @author davidroussel
 */
public class UserMessage extends Message implements Comparable<UserMessage>, Serializable
{
	/**
	 * L'auteur du message
	 */
	private String author;

	/**
	 * Ordres de tri des messages.
	 * Ces critères seront utilisé lors de la comparaison de deux
	 * messages.
	 */
	public enum Order
	{
		AUTHOR,
		DATE,
		CONTENT;
	}

	/**
	 * Ensemble des critères de tri [Initialisé à la date seule]
	 * Les critères de tri peuvent contenir une et une seule instance
	 * des différents éléments de {@link Order} dans n'importe quel
	 * ordre.
	 */
	private static Set<Order> orders = EnumSet.of(Order.DATE);

	/**
	 * Ajout d'un critère de tri aux critères de tri
	 * @param o le critère à ajouter
	 * @return true si le critère de tri n'était pas déjà présent dans
	 * l'ensemble et qu'il a pu être ajouté, false sinon.
	 */
	public static boolean addOrder(Order o)
	{
		return orders.add(o);
	}

	/**
	 * Retrait d'un critère de tri aux critères de tri
	 * @param o le critère de tri à retirer
	 * @return true si le crière de tri était présent dans l'ensemble des
	 * critères et qu'il a été retiré, false sinon.
	 */
	public static boolean removeOrder(Order o)
	{
		return orders.remove(o);
	}

	/**
	 * Effacement de l'ensemble des critères de tri
	 */
	public static void clearOrders()
	{
		orders.clear();
	}

	/**
	 * Constructeur valué d'un message
	 * @param date la date d'arrivée du message
	 * @param author l'auteur du message
	 * @param content le contenu du message
	 */
	public UserMessage(Date date, String author, String content)
	{
		super(date, content);
		this.author = author;
	}

	/**
	 * Constructeur valué d'un message.
	 * La date d'arrivée est implicitement initialisée à "maintenant" en
	 * utilisant le calendrier
	 * @param author l'auteur du message
	 * @param content le contenu du message
	 * @see Calendar#getInstance()
	 * @see Calendar#getTime()
	 */
	public UserMessage(String author, String content)
	{
		super(content);
		this.author = author;
	}

	/**
	 * Accesseur en lecture de l'auteur du message
	 * @return l'auteur du message
	 */
	public String getAuthor()
	{
		return author;
	}

	/**
	 * Comparaison (3-way) avec un autre message
	 * @param m le message à comparer
	 */
	@Override
	public int compareTo(UserMessage m)
	{
		if (orders.isEmpty())
		{
			// l'ordre par défaut est la date du message
			return date.compareTo(m.date);
		}
		else
		{
			for (Iterator<Order> it = orders.iterator(); it.hasNext();)
			{
				Order criterium = it.next();
				int compare = 0;
				switch (criterium)
				{
					case AUTHOR:
						compare = author.compareTo(m.author);
						break;
					case DATE:
						compare = date.compareTo(m.date);
						break;
					case CONTENT:
						compare = content.compareTo(m.content);
					default:
						break;
				}
				// Si le critère courant permet de différentier les messages
				// on renvoie sa valeur tout de suite.
				if (compare != 0)
				{
					return compare;
				}
			}
			// On a terminé la boucle sans avoir renvoyé une valeur != 0,
			// tous les critères de comparaison ont été 0 (valeurs égales)
			return 0;
		}
	}

	/**
	 * @return le hashcode du message basé sur le hashcode de sa date, de son
	 * auteur et de son contenu (evt utilisé dans un hashset de messages)
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int hash = date.hashCode();
		hash = (prime * hash) + author.hashCode();
		hash = (prime * hash) + content.hashCode();
		return hash;
	}

	/**
	 * Comparaison binaire avec un autre objet
	 * @param obj l'autre objet à comparer
	 * @return true si l'autre objet est un message avec les mêmes attributs
	 * @note on peut utiliser la comparaison 3-way pour effectivement comparer
	 * deux messages;
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
		{
			return false;
		}

		if (obj == this)
		{
			return true;
		}

		if (obj instanceof UserMessage)
		{
			UserMessage m = (UserMessage) obj;

			return compareTo(m) == 0;
		}

		return false;
	}

	/**
	 * Affichage du message sous forme de chaîne de caractères
	 * @return une chaîne de caractère représentant le message sous la forme
	 * [yyyy/mm/dd HH:MM:SS] author > message content
	 */
	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer("[");

		sb.append(dateFormat.format(date));
		sb.append("] ");
		sb.append(author);
		sb.append(" > ");
		sb.append(content);

		return sb.toString();
	}
}
