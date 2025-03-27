package org.doan;
import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JFrame {
    private DefaultListModel<String> reviewListModel;
    private JLabel lblTieuDe;
    private JLabel lblThongTinSanPham;
    private JTextField textInfor;
    private JPanel panelSanPham;
    private JPanel panelButton;
    private JButton btnTiki;
    private JButton btnShoppe;
    private JButton btnLazada;
    private JButton btnGia;
    private JTable table;
    private JScrollPane tableScrollPane;

    public GUI() {
        setTitle("Tổng Hợp Review Sản Phẩm");
        setSize(1333, 664);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setLayout(null);

        lblTieuDe = new JLabel("TỔNG HỢP REVIEW SẢN PHẨM");
        lblTieuDe.setFont(new Font("Times New Roman", Font.BOLD, 20));
        lblTieuDe.setForeground(Color.BLUE);
        lblTieuDe.setBounds(471, 8, 350, 31);
        getContentPane().add(lblTieuDe);

        lblThongTinSanPham = new JLabel("Nhập tên sản phẩm :");
        lblThongTinSanPham.setFont(new Font("Arial", Font.BOLD, 14));
        lblThongTinSanPham.setBounds(10, 45, 160, 31);
        getContentPane().add(lblThongTinSanPham);

        textInfor = new JTextField();
        textInfor.setBounds(242, 49, 709, 25);
        getContentPane().add(textInfor);
        textInfor.setColumns(10);

        JButton btnNewButton = new JButton("Tìm kiếm");
        btnNewButton.setFont(new Font("Arial", Font.BOLD, 14));
        btnNewButton.setBackground(Color.DARK_GRAY);
        btnNewButton.setForeground(Color.WHITE);
        btnNewButton.setBounds(1029, 45, 120, 30);
        btnNewButton.setFocusPainted(false);
        getContentPane().add(btnNewButton);

        panelSanPham = new JPanel();
        panelSanPham.setBounds(10, 86, 1299, 354);
        panelSanPham.setBorder(new LineBorder(Color.BLACK, 2));
        getContentPane().add(panelSanPham);
        panelSanPham.setLayout(new BorderLayout());

        panelButton = new JPanel();
        panelButton.setBorder(new LineBorder(Color.GRAY, 1));
        panelSanPham.add(panelButton, BorderLayout.NORTH);
        panelButton.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        btnTiki = new JButton("TIKI");
        formatButton(btnTiki);
        panelButton.add(btnTiki);

        btnShoppe = new JButton("SHOPEE");
        formatButton(btnShoppe);
        panelButton.add(btnShoppe);

        btnLazada = new JButton("LAZADA");
        formatButton(btnLazada);
        panelButton.add(btnLazada);

        btnGia = new JButton("BIẾN ĐỘNG GIÁ");
        formatButton(btnGia);
        panelButton.add(btnGia);

        String[] columnNames = {"ID Reviewer", "Rate", "Comments", "Ảnh", "Video"};
        Object[][] data = {};
        table = new JTable(data, columnNames);
        table.setGridColor(Color.BLACK);
        table.setShowGrid(true);

        tableScrollPane = new JScrollPane(table);
        panelSanPham.add(tableScrollPane, BorderLayout.CENTER);

        JButton btnOverView = new JButton("OVERVIEW");
        btnOverView.setFont(new Font("Arial", Font.BOLD, 14));
        btnOverView.setBackground(Color.DARK_GRAY);
        btnOverView.setForeground(Color.WHITE);
        btnOverView.setBounds(581, 450, 120, 30);
        btnOverView.setFocusPainted(false);
        getContentPane().add(btnOverView);

        JLabel lblOverView = new JLabel("");
        lblOverView.setBorder(new LineBorder(Color.BLACK, 1));
        lblOverView.setBounds(10, 490, 1299, 96);
        getContentPane().add(lblOverView);

        reviewListModel = new DefaultListModel<>();
        setVisible(true);
    }

    private void formatButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GUI::new);
    }
}
