package org.example;

import java.awt.*;
import java.sql.Connection;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class Paneloperateur extends JPanel {
    private static final Logger LOGGER = Logger.getLogger(Paneloperateur.class.getName());
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final String[] ALLOWED_ROLES = {"Admin", "Caissier", "Serveur"};

    private final Connection conn;
    private DefaultTableModel tab;
    private JTable table;

    public Paneloperateur(Connection conn) {
        this.conn = conn;
        initializeUI();
        afficherop();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());

        String[] colonnes = {"Nom operateur", "Groupe"};
        tab = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Empêche l'édition directe
            }
        };
        table = new JTable(tab);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnAjouter = new JButton("Ajouter");
        JButton btnChanger = new JButton("Modifier operateur");
        JButton btnSupprimer = new JButton("Supprimer");

        panel.add(btnAjouter);
        panel.add(btnChanger);
        panel.add(btnSupprimer);


        btnAjouter.addActionListener(e -> ajouterOperateur());
        btnChanger.addActionListener(e -> changerOperateur());
        btnSupprimer.addActionListener(e -> supprimerOperateur());

        add(panel, BorderLayout.NORTH);
    }

    public void afficherop() {
        tab.setRowCount(0);
        String sql = "SELECT o.nom_operateur, g.nom_groupe FROM operateur o " +
                "LEFT JOIN groupe g ON o.groupe_op = g.id ORDER BY o.nom_operateur";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                tab.addRow(new Object[]{
                        rs.getString("nom_operateur"),
                        rs.getString("nom_groupe")
                });
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'affichage des opérateurs", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement des données",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void supprimerOperateur() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un opérateur à supprimer",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nomOperateur = (String) tab.getValueAt(selectedRow, 0);

        int confirmation = JOptionPane.showConfirmDialog(this,
                "Êtes-vous sûr de vouloir supprimer l'opérateur '" + nomOperateur + "' ?",
                "Confirmation de suppression",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "DELETE FROM operateur WHERE nom_operateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomOperateur);
            int rowsAffected = ps.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Opérateur supprimé avec succès",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                afficherop();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Aucun opérateur trouvé avec ce nom",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la suppression", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la suppression : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void ajouterOperateur() {
        try {

            String nom = demanderNomOperateur();
            if (nom == null) return;


            if (operateurExiste(nom)) {
                JOptionPane.showMessageDialog(this,
                        "Un opérateur avec ce nom existe déjà",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }


            String motDePasse = demanderMotDePasse();
            if (motDePasse == null) return;


            String role = demanderRole();
            if (role == null) return;


            int groupeId = obtenirIdGroupe(role);
            if (groupeId == -1) {
                JOptionPane.showMessageDialog(this,
                        "Rôle invalide dans la base de données",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }


            insererOperateur(nom, motDePasse, groupeId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout d'opérateur", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de l'ajout : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String demanderNomOperateur() {
        String nom;
        do {
            nom = JOptionPane.showInputDialog(this,
                    "Entrez le nom de l'opérateur (minimum 3 caractères) :");

            if (nom == null) return null; // Annulation

            nom = nom.trim();
            if (nom.length() < 3) {
                JOptionPane.showMessageDialog(this,
                        "Le nom doit contenir au moins 3 caractères",
                        "Nom invalide", JOptionPane.WARNING_MESSAGE);
                nom = "";
            } else if (!nom.matches("^[a-zA-Z0-9_]+$")) {
                JOptionPane.showMessageDialog(this,
                        "Le nom ne peut contenir que des lettres, chiffres et underscore",
                        "Nom invalide", JOptionPane.WARNING_MESSAGE);
                nom = "";
            }
        } while (nom.isEmpty());

        return nom;
    }

    private String demanderMotDePasse() {
        JPasswordField passwordField = new JPasswordField();
        JPasswordField confirmPasswordField = new JPasswordField();

        Object[] message = {
                "Mot de passe (min. " + MIN_PASSWORD_LENGTH + " caractères):", passwordField,
                "Confirmer le mot de passe:", confirmPasswordField
        };

        int option = JOptionPane.showConfirmDialog(this, message,
                "Saisie du mot de passe", JOptionPane.OK_CANCEL_OPTION);

        if (option != JOptionPane.OK_OPTION) return null;

        String motDePasse = new String(passwordField.getPassword());
        String confirmation = new String(confirmPasswordField.getPassword());

        // Validation du mot de passe
        if (motDePasse.length() < MIN_PASSWORD_LENGTH) {
            JOptionPane.showMessageDialog(this,
                    "Le mot de passe doit contenir au moins " + MIN_PASSWORD_LENGTH + " caractères",
                    "Mot de passe invalide", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!motDePasse.equals(confirmation)) {
            JOptionPane.showMessageDialog(this,
                    "Les mots de passe ne correspondent pas",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        // Validation de la complexité
        if (!estMotDePasseComplexe(motDePasse)) {
            JOptionPane.showMessageDialog(this,
                    "Le mot de passe doit contenir au moins :\n" +
                            "- Une lettre majuscule\n" +
                            "- Une lettre minuscule\n" +
                            "- Un chiffre\n" +
                            "- Un caractère spécial",
                    "Mot de passe trop simple", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        return motDePasse;
    }

    private boolean estMotDePasseComplexe(String motDePasse) {
        return motDePasse.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$");
    }

    private String demanderRole() {
        return (String) JOptionPane.showInputDialog(this,
                "Choisissez le rôle :",
                "Sélection du rôle",
                JOptionPane.QUESTION_MESSAGE,
                null, ALLOWED_ROLES, ALLOWED_ROLES[0]);
    }

    private boolean operateurExiste(String nom) throws SQLException {
        String sql = "SELECT COUNT(*) FROM operateur WHERE nom_operateur = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private int obtenirIdGroupe(String nomGroupe) throws SQLException {
        String sql = "SELECT id FROM groupe WHERE nom_groupe = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nomGroupe);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return -1;
    }

    private void insererOperateur(String nom, String motDePasse, int groupeId) throws SQLException {
        String sql = "INSERT INTO operateur (nom_operateur, password, groupe_op) VALUES (?, ?, ?)";
        String motDePasseHache = hachageMotDePasse(motDePasse);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setString(2, motDePasseHache);
            ps.setInt(3, groupeId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Opérateur ajouté avec succès",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                afficherop();
            }
        }
    }

    private String hachageMotDePasse(String motDePasse) {
        try {
            // Génération d'un salt aléatoire
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[16];
            random.nextBytes(salt);

            // Hachage avec SHA-256
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(motDePasse.getBytes());

            // Conversion en hexadécimal avec salt
            StringBuilder sb = new StringBuilder();
            for (byte b : salt) {
                sb.append(String.format("%02x", b));
            }
            sb.append(":");
            for (byte b : hashedPassword) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "Algorithme de hachage non disponible", e);
            throw new RuntimeException("Erreur de sécurité", e);
        }
    }

    private void changerOperateur() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Veuillez sélectionner un opérateur à modifier",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nomOperateur = (String) tab.getValueAt(selectedRow, 0);

        try {
            // Demander le nouveau rôle
            String nouveauRole = demanderRole();
            if (nouveauRole == null) return;

            // Demander si on change aussi le mot de passe
            int changeMdp = JOptionPane.showConfirmDialog(this,
                    "Voulez-vous également changer le mot de passe ?",
                    "Changement de mot de passe",
                    JOptionPane.YES_NO_OPTION);

            String nouveauMotDePasse = null;
            if (changeMdp == JOptionPane.YES_OPTION) {
                nouveauMotDePasse = demanderMotDePasse();
                if (nouveauMotDePasse == null) return;
            }

            // Mise à jour en base
            modifierOperateur(nomOperateur, nouveauRole, nouveauMotDePasse);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la modification", e);
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la modification : " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void modifierOperateur(String nom, String nouveauRole, String nouveauMotDePasse) throws SQLException {
        int groupeId = obtenirIdGroupe(nouveauRole);
        if (groupeId == -1) {
            JOptionPane.showMessageDialog(this,
                    "Rôle invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql;
        if (nouveauMotDePasse != null) {
            sql = "UPDATE operateur SET password = ?, groupe_op = ? WHERE nom_operateur = ?";
        } else {
            sql = "UPDATE operateur SET groupe_op = ? WHERE nom_operateur = ?";
        }

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (nouveauMotDePasse != null) {
                ps.setString(1, hachageMotDePasse(nouveauMotDePasse));
                ps.setInt(2, groupeId);
                ps.setString(3, nom);
            } else {
                ps.setInt(1, groupeId);
                ps.setString(2, nom);
            }

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this,
                        "Opérateur modifié avec succès",
                        "Succès", JOptionPane.INFORMATION_MESSAGE);
                afficherop();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Aucun opérateur trouvé avec ce nom",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Méthode utilitaire pour valider l'entrée utilisateur
    private boolean estNomValide(String nom) {
        return nom != null &&
                nom.trim().length() >= 3 &&
                nom.matches("^[a-zA-Z0-9_]+$");
    }

    // Méthode pour nettoyer les ressources si nécessaire
    public void fermer() {
        // Nettoyage des ressources sensibles
        if (tab != null) {
            tab.setRowCount(0);
        }
    }
}