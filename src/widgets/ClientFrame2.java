package widgets;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.StyleConstants;

import java.io.ObjectInputStream;
import java.io.IOException;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import chat.Failure;
import chat.Vocabulary;

import models.Message;
import models.NameSetListModel;

public class ClientFrame2 extends AbstractClientFrame
{
	/**
	 * Lecteur de flux d'entrée. Lit les données texte du {@link #inPipe} pour
	 * les afficher dans le {@link #document}
	 */
	private ObjectInputStream inOS;

	/**
	 * Le label indiquant sur quel serveur on est connecté
	 */
	protected final JLabel serverLabel;

	/**
	 * La zone du texte à envoyer
	 */
	protected final JTextField sendTextField;

	/**
	 * Actions à réaliser lorsque l'on veut effacer le contenu du document
	 */
	private final ClearAction clearAction;

	/**
	 * Actions à réaliser lorsque l'on veut envoyer un message au serveur
	 */
	private final SendAction sendAction;

	/**
	 * Actions à réaliser lorsque l'on veut envoyer un message au serveur
	 */
	protected final QuitAction quitAction;

	/**
	 * Actions à réaliser lorsque l'on veut supprimer les messages sélectionnés
	 */
	protected final ClearSelectedAction clearSelectedAction;

	/**
	 * Actions à réaliser lorsque l'on veut kicker les utilisateurs sélectionnés
	 */
	protected final KickSelectedAction kickSelectedAction;

	/**
	 * Actions à réaliser lorsque l'on veut filtrer les messages des utilisateurs sélectionnés
	 */
	protected final FilterSelectedAction filterSelectedAction;

	/**
	 * Actions à réaliser lorsque l'on veut trier les messages par date
	 */
	protected final SortAction sortActionDate;

	/**
	 * Actions à réaliser lorsque l'on veut trier les messages par auteur
	 */
	protected final SortAction sortActionAuthor;

	/**
	 * Actions à réaliser lorsque l'on veut trier les messages par contenu
	 */
	protected final SortAction sortActionContent;

	/**
	 * Référence à la fenêtre courante (à utiliser dans les classes internes)
	 */
	protected final JFrame thisRef;


	private JPopupMenu popupMenu;

	private JCheckBoxMenuItem filterMenuItem;

	private JToggleButton filterButton;

	private Vector<Integer> selectedUsers;

	protected Vector<Message> storedMessage;	

	private String nameUser;

	private ListSelectionModel selectionModel;

	NameSetListModel userListModel = new NameSetListModel();

	/**
	 * Constructeur de la fenêtre
	 * @param name le nom de l'utilisateur
	 * @param host l'hôte sur lequel on est connecté
	 * @param commonRun état d'exécution des autres threads du client
	 * @param parentLogger le logger parent pour les messages
	 * @throws HeadlessException
	 */
	public ClientFrame2(String name,
										 String host,
										 Boolean commonRun,
										 Logger parentLogger)
					throws HeadlessException
	{
		super(name, host, commonRun, parentLogger);
		thisRef = this;

		storedMessage = new Vector<>();
		selectedUsers = new Vector<>();
		nameUser = name;

		// --------------------------------------------------------------------
		// Flux d'IO
		//---------------------------------------------------------------------
	/*
	 * Attention, la création du flux d'entrée doit (éventuellement) être
	 * reportée jusqu'au lancement du run dans la mesure où le inPipe
	 * peut ne pas encore être connecté à un PipedOutputStream
	 */

		// --------------------------------------------------------------------
		// Création des actions send, clear et quit
		// --------------------------------------------------------------------

		sendAction = new SendAction();
		clearAction = new ClearAction();
		quitAction = new QuitAction();

		clearSelectedAction = new ClearSelectedAction();
		kickSelectedAction = new KickSelectedAction();
		filterSelectedAction = new FilterSelectedAction();

		sortActionDate = new SortAction("Date");
		sortActionAuthor = new SortAction("Author");
		sortActionContent = new SortAction("Content");

		addWindowListener(new FrameWindowListener());

		// --------------------------------------------------------------------
		// Widgets setup (handled by Window builder)
		// --------------------------------------------------------------------
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		getContentPane().add(toolBar, BorderLayout.NORTH);

		JButton quitButton = new JButton(quitAction);
		quitButton.setHideActionText(true);
		toolBar.add(quitButton);

		toolBar.add(Box.createRigidArea(new Dimension(20,0)));

		JButton clearSelectedButton = new JButton(clearSelectedAction);
		clearSelectedButton.setHideActionText(true);
		toolBar.add(clearSelectedButton);

		JButton kickSelectedButton = new JButton(kickSelectedAction);
		kickSelectedButton.setHideActionText(true);
		toolBar.add(kickSelectedButton);

		toolBar.add(Box.createRigidArea(new Dimension(20,0)));

		JButton clearButton = new JButton(clearAction);
		clearButton.setHideActionText(true);
		toolBar.add(clearButton);

		filterButton = new JToggleButton(filterSelectedAction);
		filterButton.setHideActionText(true);
		toolBar.add(filterButton);

		Component toolBarSep = Box.createHorizontalGlue();
		toolBar.add(toolBarSep);

		serverLabel = new JLabel(host == null ? "" : host);
		toolBar.add(serverLabel);


		JPanel sendPanel = new JPanel();
		getContentPane().add(sendPanel, BorderLayout.SOUTH);


		sendPanel.setLayout(new BorderLayout(1, 0));
		sendTextField = new JTextField(" Nouveau message à envoyer");
		sendTextField.setAction(sendAction);
		sendPanel.add(sendTextField);
		sendTextField.setColumns(0);

		JButton sendButton = new JButton(sendAction);
		sendButton.setHideActionText(true);
		sendPanel.add(sendButton, BorderLayout.EAST);

		JPanel centerPanel = new JPanel();
		getContentPane().add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new GridLayout(1, 2));

		JScrollPane scrollPaneUser = new JScrollPane();
		centerPanel.add(scrollPaneUser);

		JScrollPane scrollPaneMessage = new JScrollPane();
		centerPanel.add(scrollPaneMessage);

		JTextPane textPane = new JTextPane();
		textPane.setEditable(false);
		// autoscroll textPane to bottom
		DefaultCaret caret = (DefaultCaret) textPane.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

		scrollPaneMessage.setViewportView(textPane);

		JList<String> userList = new JList<>();
		userList.setModel(userListModel);
		userListModel.add(name);
		userList.setCellRenderer(new ColorTextRenderer());
		scrollPaneUser.setViewportView(userList);

		popupMenu = new JPopupMenu();
		popupMenu.add(clearSelectedAction);
		popupMenu.add(kickSelectedAction);

		MouseListener popupListener = new PopupListener();
		userList.addMouseListener(popupListener);

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		JMenu connectionsMenu = new JMenu("Connections");
		menuBar.add(connectionsMenu);

		JMenu messagesMenu = new JMenu("Messages");
		menuBar.add(messagesMenu);

		JMenu usersMenu = new JMenu("Users");
		menuBar.add(usersMenu);

		JMenuItem quitMenuItem = new JMenuItem(quitAction);
		connectionsMenu.add(quitMenuItem);

		JMenuItem clearMenuItem = new JMenuItem(clearAction);
		messagesMenu.add(clearMenuItem);

		filterMenuItem = new JCheckBoxMenuItem(filterSelectedAction);
		messagesMenu.add(filterMenuItem);

		JMenu sortMenu = new JMenu("Sort");
		sortMenu.setMnemonic(KeyEvent.VK_S);
		messagesMenu.add(sortMenu);
		JMenuItem sortDate = new JMenuItem(sortActionDate);
		sortMenu.add(sortDate);
		JMenuItem sortAuthor = new JMenuItem(sortActionAuthor);
		sortMenu.add(sortAuthor);
		JMenuItem sortContent = new JMenuItem(sortActionContent);
		sortMenu.add(sortContent);

		JMenuItem clearSelectedMenuItem = new JMenuItem(clearSelectedAction);
		usersMenu.add(clearSelectedMenuItem);

		JMenuItem kickMenuItem = new JMenuItem(kickSelectedAction);
		usersMenu.add(kickMenuItem);

		filterSelectedAction.setEnabled(false);
		clearSelectedAction.setEnabled(false);
		kickSelectedAction.setEnabled(false);

		// --------------------------------------------------------------------
		// Documents
		// récupération du document du textPane ainsi que du documentStyle et du
		// defaultColor du document
		//---------------------------------------------------------------------
		document = textPane.getStyledDocument();
		documentStyle = textPane.addStyle("New Style", null);
		defaultColor = StyleConstants.getForeground(documentStyle);

		selectionModel = userList.getSelectionModel();
		selectionModel.addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent lse)
			{
				ListSelectionModel lsm = (ListSelectionModel) lse.getSource();

				int firstIndex = lse.getFirstIndex();
				int lastIndex = lse.getLastIndex();
				boolean isAdjusting = lse.getValueIsAdjusting();
				selectedUsers = new Vector<>();

				if(!isAdjusting)
				{
					if(lsm.isSelectionEmpty())
					{
						filterSelectedAction.setEnabled(false);
						clearSelectedAction.setEnabled(false);
						kickSelectedAction.setEnabled(false);
					}
					else
					{
						filterSelectedAction.setEnabled(true);
						clearSelectedAction.setEnabled(true);
						kickSelectedAction.setEnabled(true);
						int minSelectionIndex = lsm.getMinSelectionIndex();
						int maxSelectionIndex = lsm.getMaxSelectionIndex();
						for (int i = minSelectionIndex; i <= maxSelectionIndex ; i++)
						{
							if(lsm.isSelectedIndex(i))
							{
									selectedUsers.add(i);
							}
						}
					}
				}
			}
		});
		sendTextField.addFocusListener(new FocusListener()
		{
			public void focusGained(FocusEvent e)
			{
			 	sendTextField.setText("");
			}

			public void focusLost(FocusEvent e)
			{
				sendTextField.setText(" Nouveau message à envoyer");
			}
		});
	}

	protected void displayMessage(Message message)
	{
		String msg = message.getAuthor();
		if((msg != null) && (msg.length() > 0))
		{
			StyleConstants.setForeground(documentStyle,
						new Color(msg.hashCode()).darker());
		}
		try
		{
			document.insertString(document.getLength(), message.toString() + Vocabulary.newLine, documentStyle);
		}
		catch (BadLocationException e)
		{
			logger.warning("ClientFrame2: bad location");
		}
		StyleConstants.setForeground(documentStyle, defaultColor);

	}

	/**
	 * Listener lorsque le bouton #btnClear est activé. Efface le contenu du
	 * {@link #document}
	 */
	protected class ClearAction extends AbstractAction
	{

		public ClearAction()
		{
			putValue(SMALL_ICON,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/erase2-16.png")));
			putValue(LARGE_ICON_KEY,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/erase2-32.png")));
			putValue(ACCELERATOR_KEY,
								KeyStroke.getKeyStroke(KeyEvent.VK_L,
												InputEvent.META_MASK));
			putValue(NAME, "Clear messages");
			putValue(NAME, "Clear all messages");
		}

		/**
		 * Opérations réalisées lorsque l'action est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			try
			{
				document.remove(0, document.getLength());
				storedMessage = new Vector<>();
			}
			catch (BadLocationException e)
			{
				logger.warning("ClientFrame: bad location");
				logger.warning(e.getLocalizedMessage());
			}
		}
	}
	protected class SendAction extends AbstractAction
	{
		public SendAction()
		{
			putValue(SMALL_ICON,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/sent-16.png")));
			putValue(LARGE_ICON_KEY,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/sent-32.png")));
			putValue(ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_S,
											InputEvent.META_MASK));
			putValue(NAME, "Send");
			putValue(SHORT_DESCRIPTION, "Send text to server");
		}

		/**
		 * Opérations réalisées lorsque l'action est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			String content = sendTextField.getText();
			if(content != null)
			{
				if(content.length() > 0)
				{
					sendMessage(content);
					sendTextField.setText("");
				}
			}
		}
	}
	private class QuitAction extends AbstractAction
	{
		public QuitAction()
		{
				putValue(SMALL_ICON,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/disconnected-16.png")));
				putValue(LARGE_ICON_KEY,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/disconnected-32.png")));
				putValue(ACCELERATOR_KEY,
								KeyStroke.getKeyStroke(KeyEvent.VK_Q,
												InputEvent.META_MASK));
				putValue(NAME, "Quit");
				putValue(SHORT_DESCRIPTION, "Disconnect from server and quit");
		}

		/**
		 * Opérations réalisées lorsque l'action "quitter" est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			logger.info("QuitAction: sending bye ... ");
			serverLabel.setText("");
			thisRef.validate();

			try
			{
				Thread.sleep(1000);
			}
			catch (InterruptedException e)
			{
				return;
			}
			sendMessage(Vocabulary.byeCmd);
		}
	}

	private class ClearSelectedAction extends AbstractAction{
		public ClearSelectedAction()
		{
			putValue(SMALL_ICON,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/delete_database-16.png")));
			putValue(LARGE_ICON_KEY,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/delete_database-32.png")));
			putValue(ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_F,
											InputEvent.META_MASK));
			putValue(NAME, "Clear selected");
			putValue(SHORT_DESCRIPTION, "Clear the selected messages");
		}

		/**
		 * Opérations réalisées lorsque l'action "supprimer les messages des auteurs sélectionnés" est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			try
			{
					document.remove(0, document.getLength());
			}
			catch (BadLocationException e)
			{
					logger.warning("ClientFrame2: clear doc: bad location");
					logger.warning(e.getLocalizedMessage());
			}

			Vector<Message> remainingMsg = new Vector<>();

			for (Message msg : storedMessage)
			{
					if(msg.hasAuthor())
					{
							if(!selectedUsers.contains(userListModel.indexOf(msg.getAuthor())))
							{
									remainingMsg.add(msg);
							}
					}
			}

			storedMessage = new Vector<>(remainingMsg);

			Consumer<Message> msgPrinter = (Message msg) -> displayMessage(msg);

			if(filterButton.isSelected())
			{
					Predicate<Message> selectionFilter = (Message msg) ->
					{
							if(msg != null)
							{
									if(msg.hasAuthor())
									{
											if(selectedUsers.contains(userListModel.indexOf(msg.getAuthor())))
											{
													return true;
											}
									}
							}
							return false;
					};

					storedMessage.stream().sorted().filter(selectionFilter).forEach(msgPrinter);
			}
			else
			{
					storedMessage.stream().sorted().forEach(msgPrinter);
			}
		}
	}
	private class KickSelectedAction extends AbstractAction{
			public KickSelectedAction()
			{
				putValue(SMALL_ICON,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/remove_user-16.png")));
				putValue(LARGE_ICON_KEY,
								new ImageIcon(ClientFrame2.class
												.getResource("/icons/remove_user-32.png")));
				putValue(ACCELERATOR_KEY,
								KeyStroke.getKeyStroke(KeyEvent.VK_M,
												InputEvent.META_MASK));
				putValue(NAME, "Kick selected");
				putValue(SHORT_DESCRIPTION, "Kick selected user(s)");
			}

			/**
			 * Opérations réalisées lorsque l'action "kicker les utilisateurs sélectionnés" est sollicitée
			 * @param e évènement à l'origine de l'action
			 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
			 */
			@Override
			public void actionPerformed(ActionEvent evt)
			{
				for(int i = 0; i < selectedUsers.size(); i++)
				{
					String currentUser = userListModel.getElementAt(i);
					if(!currentUser.equals(nameUser)) 
							outPW.println("Kick " + currentUser);
				}
			}

	}

	private class FilterSelectedAction extends AbstractAction
	{
		public FilterSelectedAction()
		{
			putValue(SMALL_ICON,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/filled_filter-16.png")));
			putValue(LARGE_ICON_KEY,
							new ImageIcon(ClientFrame2.class
											.getResource("/icons/filled_filter-32.png")));
			putValue(ACCELERATOR_KEY,
							KeyStroke.getKeyStroke(KeyEvent.VK_N,
											InputEvent.META_MASK));
			putValue(NAME, "Filter selected");
			putValue(SHORT_DESCRIPTION, "Filter the selected messages");
		}

		/**
		 * Opérations réalisées lorsque l'action "filtrer les messages des utilisateurs sélectionnés" est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			AbstractButton source = (AbstractButton) evt.getSource();

			try
			{
				document.remove(0, document.getLength());
			}
			catch (BadLocationException e)
			{
				logger.warning("ClientFrame: bad location");
				logger.warning(e.getLocalizedMessage());
			}

			Consumer<Message> msgPrinter = (Message msg) -> displayMessage(msg);

			if(source.isSelected())
			{
				filterMenuItem.setSelected(true);
				filterButton.setSelected(true);

				Predicate<Message> selectionFilter = (Message msg) ->
				{
						if(msg != null)
						{
								if(msg.hasAuthor())
								{
										if(selectedUsers.contains(userListModel.indexOf(msg.getAuthor())))
										{
												return true;
										}
								}
						}
						return false;
				};
				storedMessage.stream().sorted().filter(selectionFilter).forEach(msgPrinter);
			}
			else
			{
					filterMenuItem.setSelected(false);
					filterButton.setSelected(false);
					storedMessage.stream().sorted().forEach(msgPrinter);
			}
		}

	}

	private class SortAction extends AbstractAction{

		boolean date = true;
		boolean author = true;
		boolean content = true;

		public SortAction(String str)
		{
			putValue(NAME, str);
			putValue(SHORT_DESCRIPTION, "Sort the messages by " + str);
			if(str.equals("Date"))
					date = true;
			if(str.equals("Author"))
					author = true;
			if(str.equals("Content"))
					content = true;
		}

		/**
		 * Opérations réalisées lorsque l'action "filtrer les messages des utilisateurs sélectionnés" est sollicitée
		 * @param e évènement à l'origine de l'action
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		@Override
		public void actionPerformed(ActionEvent evt)
		{
			Consumer<Message> msgPrinter = (Message msg) -> displayMessage(msg);

			if(date)
			{
					Message.removeOrder(Message.MessageOrder.AUTHOR);
					Message.removeOrder(Message.MessageOrder.CONTENT);
					Message.addOrder(Message.MessageOrder.DATE);
			}
			else if(author)
			{
					Message.removeOrder(Message.MessageOrder.CONTENT);
					Message.removeOrder(Message.MessageOrder.DATE);
					Message.addOrder(Message.MessageOrder.AUTHOR);
			}
			else if(content)
			{
					Message.removeOrder(Message.MessageOrder.DATE);
					Message.removeOrder(Message.MessageOrder.AUTHOR);
					Message.addOrder(Message.MessageOrder.CONTENT);
			}
			try
			{
					document.remove(0, document.getLength());
			}
			catch (BadLocationException e)
			{
					logger.warning("ClientFrame: bad location");
					logger.warning(e.getLocalizedMessage());
			}

			if(filterButton.isSelected())
			{
				Predicate<Message> selectionFilter = (Message msg) ->
				{
					if(msg != null)
					{
						if(msg.hasAuthor())
						{
							if(selectedUsers.contains(userListModel.indexOf(msg.getAuthor())))
							{
								return true;
							}
						}
					}
					return false;
				};
				storedMessage.stream().sorted().filter(selectionFilter).forEach(msgPrinter);
			}
			else
			{
				storedMessage.stream().sorted().forEach(msgPrinter);
			}
		}
	}

	/**
	 * Classe gérant la fermeture correcte de la fenêtre. La fermeture correcte
	 * de la fenètre implique de lancer un cleanup
	 */
	protected class FrameWindowListener extends WindowAdapter
	{
		/**
		 * Méthode déclenchée à la fermeture de la fenêtre. Envoie la commande
		 * "bye" au serveur
		 */
		@Override
		public void windowClosing(WindowEvent evt)
		{
			logger.info("FrameWindowListener::windowClosing: sending bye ... ");
			if(quitAction != null)
			{
				quitAction.actionPerformed(null);
			}
		}
	}


	public static class ColorTextRenderer extends JLabel
					implements ListCellRenderer<String>
	{
		private Color color = null;

		@Override
		public Component getListCellRendererComponent(
						JList<? extends String> list, String value, int index,
						boolean isSelected, boolean cellHasFocus)
		{
			color = list.getForeground();
			if(value != null)
			{
				if(value.length() > 0)
				{
					color = new Color(value.hashCode()).brighter();
				}
			}
			setText(value);
			if(isSelected)
			{
				setBackground(color);
				setForeground(list.getSelectionForeground());
			}
			else
			{
				setBackground(list.getBackground());
				setForeground(color);
			}
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}

	//listener pour declencher le menu popup
	public class PopupListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent evt)
		{
			maybeShowPopup(evt);
		}
		public void mouseReleased(MouseEvent evt)
		{
			maybeShowPopup(evt);
		}
		private void maybeShowPopup(MouseEvent evt)
		{
			if(evt.isPopupTrigger())
			{
				popupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
			}
		}
	}

	/**
	 * Exécution de la boucle d'exécution. La boucle d'exécution consiste à lire
	 * une ligne sur le flux d'entrée avec un BufferedReader tant qu'une erreur
	 * d'IO n'intervient pas indiquant que le flux a été coupé. Auquel cas on
	 * quitte la boucle principale et on ferme les flux d'I/O avec #cleanup()
	 */
	@Override
	public void run()
	{
		try
		{
			inOS = new ObjectInputStream(inPipe);
		}
		catch (IOException e)
		{
			logger.severe(Failure.CLIENT_INPUT_STREAM
							+ " unable to get user piped in stream");
			logger.severe(e.getLocalizedMessage());
			System.exit(Failure.CLIENT_INPUT_STREAM.toInteger());
		}

		Message messageIn;

		while (commonRun.booleanValue())
		{
			messageIn = null;

			try
			{
				messageIn = (Message) inOS.readObject();
			}
			catch (IOException e)
			{
				logger.warning("ClientFrame2: io error at reading");
				break;
			}
			catch (ClassNotFoundException e)
			{
				logger.warning("ClientFrame2: class not found reading");
				break;
			}

			if(messageIn != null)
			{
				storedMessage.add(messageIn);

				if(messageIn.hasAuthor() && !userListModel.contains(messageIn.getAuthor()))
				{
						userListModel.add(messageIn.getAuthor());
				}
			}
			else
			{
				break;
			}
			try
			{
				document.remove(0, document.getLength());
			}
			catch (BadLocationException e)
			{
				logger.warning("ClientFrame: bad location");
				logger.warning(e.getLocalizedMessage());
			}
			Consumer<Message> msgPrinter = (Message msg) -> displayMessage(msg);

			if(filterButton.isSelected())
			{
				Predicate<Message> selectionFilter = (Message msg) ->
				{
					if(msg != null)
					{
						if(msg.hasAuthor())
						{
							if(selectedUsers.contains(userListModel.indexOf(msg.getAuthor())))
							{
								return true;
							}
						}
					}
					return false;
				};
				storedMessage.stream().sorted().filter(selectionFilter).forEach(msgPrinter);
			}
			else
			{
				storedMessage.stream().sorted().forEach(msgPrinter);
			}
		}
		if(commonRun.booleanValue())
		{
			logger.info("ClientFrame2::cleanup: changing run state at the end ... ");
			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}
		cleanup();
	}
	@Override
	public void cleanup()
	{
		logger.info("ClientFrame2::cleanup: closing input buffered reader ... ");
		try
		{
				inOS.close();
		}
		catch (IOException e)
		{
			logger.warning("ClientFrame2::cleanup: failed to close input reader"
							+ e.getLocalizedMessage());
		}
		super.cleanup();
	}
}
