package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.sql.*;
import java.awt.print.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PanelCommandes extends JPanel {
    private final Connection conn;
    private JPanel famillesPanel, articlesPanel, panierPanel, sallePanel, tablePanel, panelCmdEnCours;
    JButton btnespece = new JButton("Espece");
    JButton btnSupprimer = new JButton("Supprimer");
    JButton btnReset = new JButton("Reset");
    JButton btnencours=new JButton("Cmd encours");
    private JTable panierTable;
    private JLabel total;
    private JLabel salle_table;
    private DefaultTableModel panier;
    private int operateur_id;
    private String salleSelectionnee = "";
    private String tableSelectionnee = "";
    private JPanel switcherPanel;
    private int idtbs=-1;
    private String nomGroupe;
    private Integer TicketId = null;

    public PanelCommandes(Connection conn, int operateur_id,String nomGroupe) {
        this.conn = conn;
        this.operateur_id = operateur_id;
        this.nomGroupe=nomGroupe;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));


        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.setPreferredSize(new Dimension(100, 50));

        JButton btnSalle = new JButton("Salle");


        btnencours.addActionListener(e -> encours());

        topPanel.add(btnReset);
        topPanel.add(btnencours);

        topPanel.add(btnSalle);

        famillesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        sallePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JScrollPane salleScroller = new JScrollPane(sallePanel);
        JScrollPane famillesScroll = new JScrollPane(famillesPanel);
        famillesScroll.setPreferredSize(new Dimension(100, 50));
        salleScroller.setPreferredSize(new Dimension(100, 50));

        panierPanel = new JPanel(new BorderLayout());
        panierPanel.setPreferredSize(new Dimension(300, 50));
        setupPanier();

        JPanel middlePanel = new JPanel(new BorderLayout());
        middlePanel.setPreferredSize(new Dimension(200, 50));
        middlePanel.add(famillesScroll, BorderLayout.CENTER);
        middlePanel.add(panierPanel, BorderLayout.EAST);


        switcherPanel = new JPanel(new CardLayout());
        switcherPanel.setPreferredSize(new Dimension(100, 200));
        articlesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tablePanel.setPreferredSize(new Dimension(100, 200));

        switcherPanel.add(new JScrollPane(articlesPanel), "Articles");
        switcherPanel.add(new JScrollPane(tablePanel), "Tables");
        switcherPanel.add(new JScrollPane(sallePanel), "Salles");
        panelCmdEnCours = new JPanel(new FlowLayout(FlowLayout.LEFT));
        switcherPanel.add(new JScrollPane(panelCmdEnCours), "Encours");





        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(switcherPanel, BorderLayout.SOUTH);


        btnSalle.addActionListener(e -> {

            chargerSalles();
            CardLayout cl = (CardLayout) switcherPanel.getLayout();
            cl.show(switcherPanel, "Tables");
        });

        btnReset.addActionListener(e -> reset());


        chargerFamilles();
        chargerSalles();
        restriction();
    }


    private void encours() {
        panelCmdEnCours.removeAll();

        String sql = "SELECT ticket_id, table_id, SUM(prix_total) as total FROM commande WHERE etatcmd = 'validée' GROUP BY ticket_id, table_id";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int ticketId = rs.getInt("ticket_id");
                int tableId = rs.getInt("table_id");
                double total = rs.getDouble("total");


                JButton btnCmd = new JButton(
                        "Table " + tableId + " | Ticket " + ticketId + " | " + total + " DH"
                );
                btnCmd.setPreferredSize(new Dimension(250, 50));
                btnCmd.setBackground(new Color(52, 152, 219));
                btnCmd.setForeground(Color.WHITE);
                btnCmd.setFont(new Font("Arial", Font.BOLD, 14));
                btnCmd.setFocusPainted(false);
                

                btnCmd.addActionListener(e ->{


                        TicketId=ticketId;
                        chargerCommande(ticketId);});

                panelCmdEnCours.add(btnCmd);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement des commandes : " + e.getMessage(),
                    "Erreur SQL", JOptionPane.ERROR_MESSAGE);
        }

        panelCmdEnCours.revalidate();
        panelCmdEnCours.repaint();
        CardLayout cl = (CardLayout) switcherPanel.getLayout();
        cl.show(switcherPanel, "Encours");
    }



    private void chargerCommande(int ticketId) {
        panier.setRowCount(0);

        // Récupérer la table liée à ce ticket
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT table_id FROM commande WHERE ticket_id = ?")) {
            ps.setInt(1, ticketId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                idtbs = rs.getInt("table_id");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur récupération table : " + e.getMessage());
        }

        String sql = """
        SELECT a.nom_article, c.prix_unitaire, c.quantite, c.prix_total
        FROM commande c
        JOIN article a ON c.article_id = a.id
        WHERE c.ticket_id = ? AND c.etatcmd = 'validée'
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ticketId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nom = rs.getString("nom_article");
                double prix = rs.getDouble("prix_unitaire");
                int qte = rs.getInt("quantite");
                double totalLigne = rs.getDouble("prix_total");
                panier.addRow(new Object[]{nom, prix, qte, totalLigne});
            }
            calculerTotal();

            salle_table.setText("Ticket: " + ticketId + " | Table: " + idtbs);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement du panier : " + e.getMessage(),
                    "Erreur SQL", JOptionPane.ERROR_MESSAGE);
        }
    }


    private void chargerFamilles() {
        famillesPanel.removeAll();
        try {
            String sql = "SELECT * FROM famille";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String nomFamille = rs.getString("nom_famille");
                int idFamille = rs.getInt("id");

                JButton btnFamille = new JButton(nomFamille);
                btnFamille.setPreferredSize(new Dimension(120, 60));
                btnFamille.addActionListener(e -> {
                    tableSelectionnee = nomFamille;
                    chargerArticles(idFamille);
                });
                famillesPanel.add(btnFamille);
            }
            famillesPanel.revalidate();
            famillesPanel.repaint();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerArticles(int idFamille) {
        CardLayout cl = (CardLayout) switcherPanel.getLayout();
        cl.show(switcherPanel, "Articles");
        articlesPanel.removeAll();

        try {
            String sql = "SELECT * FROM article WHERE famille_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idFamille);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int articleId = rs.getInt("id");
                String nom = rs.getString("nom_article");
                double prixPrincipal = rs.getDouble("prix");

                JButton btnArticle = new JButton("<html><b>" + nom + "</b><br>" + prixPrincipal + " DH</html>");
                btnArticle.setPreferredSize(new Dimension(120, 60));

                btnArticle.addActionListener(e -> ajouterAuPanierAvecChoixPrix(nom, articleId, prixPrincipal));
                articlesPanel.add(btnArticle);
            }

            articlesPanel.revalidate();
            articlesPanel.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void ajouterAuPanierAvecChoixPrix(String nom, int articleId, double prixPrincipal) {

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        comboModel.addElement("Prix principal: " + prixPrincipal);

        try {
            String sqlPrixSup = "SELECT prix FROM article_prix WHERE article_id = ?";
            PreparedStatement ps = conn.prepareStatement(sqlPrixSup);
            ps.setInt(1, articleId);
            ResultSet rs = ps.executeQuery();
            int i = 1;
            while (rs.next()) {
                comboModel.addElement("PVP" + i + ": " + rs.getDouble("prix"));
                i++;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        JComboBox<String> comboPrix = new JComboBox<>(comboModel);
        int choix = JOptionPane.showConfirmDialog(this, comboPrix,
                "Choisir le prix pour " + nom, JOptionPane.OK_CANCEL_OPTION);

        if (choix == JOptionPane.OK_OPTION) {
            String prixChoisiStr = (String) comboPrix.getSelectedItem();
            double prixChoisi = Double.parseDouble(prixChoisiStr.split(":")[1].trim());


            boolean found = false;
            for (int i = 0; i < panier.getRowCount(); i++) {
                if (panier.getValueAt(i, 0).equals(nom)) {
                    int qte = (int) panier.getValueAt(i, 2) + 1;
                    panier.setValueAt(qte, i, 2);
                    panier.setValueAt(prixChoisi * qte, i, 3);
                    found = true;
                    break;
                }
            }
            if (!found) {
                panier.addRow(new Object[]{nom, prixChoisi, 1, prixChoisi});
            }
            calculerTotal();
        }
    }


    private void chargerSalles() {
        tablePanel.removeAll();
        try {
            String sql = "SELECT * FROM salle";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int id_salle = rs.getInt("id");
                String nom = rs.getString("nom_salle");

                JButton btnSalle = new JButton(nom);
                btnSalle.setPreferredSize(new Dimension(120, 60));
                btnSalle.addActionListener(e -> {
                    salleSelectionnee = nom;
                    tableSelectionnee = "Table : _";
                    chargerTables(id_salle);
                    salle_table.setText(" " + salleSelectionnee + " | " + tableSelectionnee);
                });
                tablePanel.add(btnSalle);
            }
            tablePanel.revalidate();
            tablePanel.repaint();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void chargerTables(int salleId) {
        tablePanel.removeAll();
        try {
            String sql = "SELECT id, nom_table, etat FROM les_tables WHERE salle_id = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, salleId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                int tableId = rs.getInt("id");
                String nom = rs.getString("nom_table");
                String etat = rs.getString("etat");

                JButton btnTable = new JButton(nom);
                btnTable.setPreferredSize(new Dimension(120, 60));
                if ("occupée".equals(etat)) {
                    btnTable.setBackground(Color.RED);
                    btnTable.setEnabled(false);
                } else {
                    btnTable.setBackground(Color.GREEN);
                    btnTable.addActionListener(e -> {
                        tableSelectionnee = nom;
                        idtbs=tableId;
                        salle_table.setText(" " + salleSelectionnee + " | " + tableSelectionnee);
                        try (PreparedStatement pst = conn.prepareStatement(
                                "UPDATE les_tables SET etat='occupée' WHERE id=?")) {
                            pst.setInt(1, tableId);
                            pst.executeUpdate();
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        chargerTables(salleId);
                    });
                }

                tablePanel.add(btnTable);
            }

            tablePanel.revalidate();
            tablePanel.repaint();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private void setupPanier() {
        String[] colonnes = {"Article", "Prix", "Quantité", "Total"};
        panier = new DefaultTableModel(colonnes, 0);
        panierTable = new JTable(panier);

        salle_table = new JLabel("Salle : | Table : ");
        total = new JLabel("Total: 0.00 DH");


        btnSupprimer.addActionListener(e -> supprimerArticle());
        JButton btnValider = new JButton("Valider");
        JPanel top = new JPanel(new BorderLayout());


        top.add(btnespece, BorderLayout.WEST);
        top.add(btnSupprimer, BorderLayout.EAST);
        top.add(btnValider, BorderLayout.CENTER);
        btnValider.addActionListener(e -> commandevalide());
        btnespece.addActionListener(e -> cmdpayer());

        JScrollPane scroll = new JScrollPane(panierTable);
        scroll.setPreferredSize(new Dimension(250, 250));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(total, BorderLayout.EAST);
        bottom.add(salle_table, BorderLayout.WEST);

        panierPanel.add(top, BorderLayout.NORTH);
        panierPanel.add(scroll, BorderLayout.CENTER);
        panierPanel.add(bottom, BorderLayout.SOUTH);
    }



    private void calculerTotal() {
        double sum = 0;
        for (int i = 0; i < panier.getRowCount(); i++) {
            sum += (double) panier.getValueAt(i, 3);
        }
        total.setText(String.format("Total: %.2f DH", sum));
    }

    private void supprimerArticle() {
        int row = panierTable.getSelectedRow();
        if (row != -1) {
            panier.removeRow(row);
            calculerTotal();
        }
    }

    private void reset() {
        panier.setRowCount(0);
        calculerTotal();
    }

    private void liberertable(int tableId) {
        String sql = "UPDATE les_tables SET etat='libre' WHERE id=?";
        try(PreparedStatement ps=conn.prepareStatement(sql)){
            ps.setInt(1,tableId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private void commandevalide() {
        String sql = "INSERT INTO commande (article_id , prix_unitaire,quantite ,prix_total ,operateur_id ,ticket_id,etatcmd,table_id) VALUES (?,?,?,?,?,?,?,?)  ";
        String sqi = "INSERT INTO ticket (date_heure,operateur_id,total_ttc ,table_id ) VALUES (?,?,?,?)";
        double ttc = 0;
        if (idtbs==-1) {
            JOptionPane.showMessageDialog(this, "La commande doit être liée à une table.");
            return;

        }

        for (int i = 0; i < panier.getRowCount(); i++) {
            ttc += (Double) panier.getValueAt(i, 3);
        }
        Timestamp now = new Timestamp(System.currentTimeMillis());
        try {
            PreparedStatement p = conn.prepareStatement(sqi, Statement.RETURN_GENERATED_KEYS);
            p.setTimestamp(1, now);
            p.setInt(2, operateur_id);
            p.setDouble(3, ttc);
            p.setInt(4, idtbs);
            p.executeUpdate();
            ResultSet rs = p.getGeneratedKeys();
            int ticketId = -1;
            if (rs.next()) {
                ticketId = rs.getInt(1);
            } else {
                throw new SQLException("Impossible de récupérer l'ID du ticket.");
            }

            for (int i = 0; i < panier.getRowCount(); i++) {

                String nom = (String) panier.getValueAt(i, 0);
                double prix = (Double) panier.getValueAt(i, 1);
                int quantite = (Integer) panier.getValueAt(i, 2);


                String sq = "SELECT id from article WHERE nom_article=?";
                PreparedStatement ps1 = conn.prepareStatement(sq);
                ps1.setString(1, nom);
                ResultSet rs1 = ps1.executeQuery();
                if (rs1.next()) {
                    int id = rs1.getInt("id");


                        try (PreparedStatement ps2 = conn.prepareStatement(sql)) {
                            ps2.setInt(1, id);
                            ps2.setDouble(2, prix);
                            ps2.setInt(3, quantite);
                            double prixTotal = prix * quantite;
                            ps2.setDouble(4, prixTotal);


                            ps2.setInt(5, operateur_id);
                            ps2.setInt(6, ticketId);
                            ps2.setString(7,"validée");
                            ps2.setInt(8, idtbs);
                            ps2.executeUpdate();
                        }


                }


            }





        } catch (SQLException e) {
            e.printStackTrace();
        }

reset();
        CardLayout cl = (CardLayout) switcherPanel.getLayout();
        cl.show(switcherPanel, "Articles");
    }
    private void cmdpayer() {




        String sql = "UPDATE commande SET etatcmd='payée' WHERE ticket_id=?";
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, TicketId);
            p.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du paiement : " + e.getMessage());
            return;
        }

        String[] lignes = new String[panier.getRowCount()];
        double ttc = 0;
        for (int i = 0; i < panier.getRowCount(); i++) {
            String nom = (String) panier.getValueAt(i, 0);
            int qte = (Integer) panier.getValueAt(i, 2);
            double prix = (Double) panier.getValueAt(i, 1);
            double totalLigne = (Double) panier.getValueAt(i, 3);
            lignes[i] = nom + " x" + qte + " @ " + prix + " = " + totalLigne + " DH";
            ttc += totalLigne;
        }

        String ticket = construireTicket("Client", lignes, ttc);

        try {
            imprimerTicket(ticket);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de l'impression du ticket : " + ex.getMessage());
        }


        if (idtbs != -1) {
            liberertable(idtbs);
        }

        reset();
        idtbs = -1;
        TicketId = null;
        salle_table.setText("Salle : | Table : ");
        CardLayout cl = (CardLayout) switcherPanel.getLayout();
        cl.show(switcherPanel, "Articles");
    }


    private String construireTicket(String client, String[] lignes, double total) {
        StringBuilder sb = new StringBuilder();
        sb.append("******** TICKET DE CAISSE ********\n");
        sb.append("Date : ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n");
        sb.append("Client : ").append(client).append("\n");
        sb.append("----------------------------------\n");
        for (String ligne : lignes) {
            sb.append(ligne).append("\n");
        }
        sb.append("----------------------------------\n");
        sb.append(String.format("TOTAL TTC : %.2f DH\n", total));

        sb.append("**********************************\n");
        return sb.toString();
    }
    private void restriction(){
        switch(nomGroupe){
            case "Admin":
                break;
            case "serveur":
               btnespece.setEnabled(false);
               btnespece.setVisible(false);
               btnSupprimer.setEnabled(false);
               btnSupprimer.setVisible(false);
               btnReset.setEnabled(false);
               btnReset.setVisible(false);

                break;
            case"caissier":
                break;



        }
    }




    private void imprimerTicket(String texte) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 10));

            int y = 15;
            for (String line : texte.split("\n")) {
                graphics.drawString(line, 0, y);
                y += 12;
            }

            return Printable.PAGE_EXISTS;
        });

        try {
            job.print();
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(this, "Erreur d'impression : " + e.getMessage());
        }

    }

}

