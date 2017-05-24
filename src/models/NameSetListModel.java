package models;

import javax.swing.AbstractListModel;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * ListModel contenant des noms uniques (toujours trié grâce à un TreeSet par
 * exemple).
 * L'accès à la liste de noms doit être thread safe (c'àd : plusieurs threads
 * peuvent accéder concurrentiellement à la liste de noms sans que celle ci se
 * retrouve dans un état incohérent) : Les modifications du Set interne se font
 * toujours dans un bloc synchronized(nameSet) {...}.
 * L'ajout ou le retrait d'un élément dans l'ensemble de nom est accompagné
 * d'un fireContentsChanged sur l'ensemble des éléments de la liste (à cause
 * du tri implicite des éléments) ce qui permet au List Model de notifier
 * tout widget dans lequel serait contenu ce ListModel.
 * @see {@link javax.swing.AbstractListModel}
 */
public class NameSetListModel extends AbstractListModel<String>
{
	/**
	 * Ensemble de noms triés
	 */
	private SortedSet<String> nameSet;

	/**
	 * Constructeur
	 */
	public NameSetListModel()
	{
		this.nameSet = new TreeSet<String>();
	}

	/**
	 * Ajout d'un élément
	 * @param value la valeur à ajouter
	 * @return true si l'élément à ajouter est non null et qu'il n'était pas
	 * déjà présent dans l'ensemble et false sinon.
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement ajouté à l'ensemble des noms
	 */
	public boolean add(String value)
	{
		if(!nameSet.contains(value) && value != null)
		{
			synchronized (nameSet)
			{
				nameSet.add(value);
				fireContentsChanged(this, 0, nameSet.size() - 1);
			}
			return true;
		}
		return false;
	}

	/**
	 * Teste si l'ensemble de noms contient le nom passé en argument
	 * @param value le nom à rechercher
	 * @return true si l'ensemble de noms contient "value", false sinon.
	 */
	public boolean contains(String value)
	{
		// TODO Replace with implementation ...
		return this.nameSet.contains(value);
	}

	/**
	 * Retrait de l'élément situé à l'index index
	 * @param index l'index de l'élément à supprimer
	 * @return true si l'élément a été supprimé, false sinon
	 * @warning Ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsqu'un nom est
	 * effectivement supprimé de l'ensemble des noms
	 */
	public boolean remove(int index)
	{
		// TODO Replace with implementation ...
		Iterator it = this.nameSet.iterator();
		boolean ret = false;
		synchronized (nameSet)
		{
			for(int count = 0; it.hasNext(); count++)
			{
				it.next();
				if(count == index)
				{
					it.remove();
					fireContentsChanged(this, 0, nameSet.size() - 1);
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

	/**
	 * Efface l'ensemble du contenu de la liste
	 * @warning ne pas oublier de faire un
	 * {@link #fireContentsChanged(Object, int, int)} lorsque le contenu est
	 * effectivement effacé (si non vide)
	 */
	public void clear()
	{
		synchronized (nameSet) 
		{
			this.nameSet.clear();
			fireContentsChanged(this, 0, nameSet.size()-1);
		}
	}

	/**
	 * Nombre d'éléments dans le ListModel
	 * @return le nombre d'éléments dans le modèle de la liste
	 * @see javax.swing.ListModel#getSize()
	 */
	@Override
	public int getSize()
	{

		return this.nameSet.size();
	}

	/**
	 * Accesseur à l'élément indexé
	 * @param l'index de l'élément recherché
	 * @return la chaine de caractère correponsdant à l'élément recherché ou
	 * bien null si celui ci n'existe pas
	 * @see javax.swing.ListModel#getElementAt(int)
	 */
	@Override
	public String getElementAt(int index)
	{
		// TODO Replace with implementation ...
		int count = 0;
		for(String elt : nameSet)
		{
			if(count == index)
			{
				return elt;
			}
			count++;
		}
		return null;
	}

	/**
	 * Représentation sous forme de chaine de caractères de la liste de
	 * noms unique et triés.
	 * @return une chaine de caractères représetant la liste des noms uniques
	 * et triés
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> it = nameSet.iterator(); it.hasNext();)
		{
			sb.append(it.next());
			if (it.hasNext())
			{
				sb.append(", ");
			}
		}
		return sb.toString();
	}
	// indexOf element s'il existe ou -1 sinon
	public int indexOf(String str)
	{
		int index = 0;
		for(String elt : nameSet)
		{
			if(elt.equals(str))
			{
				return index;
			}
			index++;
		}
		return -1;
	}
}
