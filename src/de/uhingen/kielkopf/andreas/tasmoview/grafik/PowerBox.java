package de.uhingen.kielkopf.andreas.tasmoview.grafik;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;

public class PowerBox extends JPanel {
   private static final long serialVersionUID=-8170872042920734021L;
   private JLabel            label;
   private JLabel            label_1;
   private JButton           button;
   boolean                   warAktiv        =false;
   private int               nr;
   /**
    * Create the panel.
    */
   public PowerBox() {
      setLayout(new BorderLayout());
      final JPanel panel=new JPanel();
      panel.setBorder(new TitledBorder(null, "Box", TitledBorder.LEADING, TitledBorder.TOP, null, null));
      panel.setName("panel");
      add(panel, BorderLayout.CENTER);
      panel.setLayout(new BorderLayout(0, 0));
      panel.add(getLabel(), BorderLayout.NORTH);
      panel.add(getLabel_1(), BorderLayout.CENTER);
      panel.add(getButton(), BorderLayout.SOUTH);
      panel.setPreferredSize(new Dimension(200, 250));
      setPreferredSize(new Dimension(200, 250));
   }
   public JButton getButton() {
      if (button == null) {
         button=new JButton("An/Aus");
         button.addActionListener(e -> warAktiv=true);
         button.setFont(button.getFont().deriveFont(button.getFont().getStyle() & ~Font.BOLD, 40f));
         button.setName("btnAnaus");
      }
      return button;
   }
   private JLabel getLabel() {
      if (label == null) {
         label=new JLabel("Schalter 1");
         label.setHorizontalAlignment(SwingConstants.CENTER);
         label.setFont(label.getFont().deriveFont(label.getFont().getStyle() & ~Font.BOLD, 30f));
         label.setName("lblS");
      }
      return label;
   }
   private JLabel getLabel_1() {
      if (label_1 == null) {
         label_1=new JLabel("ON");
         label_1.setHorizontalAlignment(SwingConstants.CENTER);
         label_1.setFont(label_1.getFont().deriveFont(label_1.getFont().getStyle() | Font.BOLD, 50f));
         label_1.setName("lblOn");
      }
      return label_1;
   }
   public int getNr() {
      return nr;
   }
   @Override
   public void setName(String name) {
      getLabel().setText(name);
      super.setName(name);
   }
   public void setNr(int boxNr) {
      nr=boxNr;
   }
   public void setStatus(String status) {
      getLabel_1().setText(status);
   }
}
