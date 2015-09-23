package ar.com.untref.adquisicion.arduino;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.JLabel;
import java.awt.Font;

@SuppressWarnings("serial")
public class Ventana extends JFrame {
	private JLabel labelTemperatura;

	public Ventana() {

		JButton btnIniciarMedicion = new JButton("Iniciar Medici\u00F3n");
		SwingWorker<Void, Void> mySwingWorker = new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {

				Medicion medicion = new Medicion(Ventana.this);
				medicion.comenzarMedicion();
				btnIniciarMedicion.setEnabled(false);
				return null;
			}
		};

		btnIniciarMedicion.setBounds(10, 95, 150, 25);
		getContentPane().setLayout(null);
		getContentPane().add(btnIniciarMedicion);
		
		labelTemperatura = new JLabel("");
		labelTemperatura.setFont(new Font("Tahoma", Font.PLAIN, 15));
		labelTemperatura.setBounds(189, 82, 200, 50);
		getContentPane().add(labelTemperatura);
		
		JLabel lblNewLabel = new JLabel("Temperatura:");
		lblNewLabel.setBounds(227, 23, 93, 50);
		getContentPane().add(lblNewLabel);
		
		btnIniciarMedicion.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				mySwingWorker.execute();
			}
		});
	}
	
	public void mostrarTemperatura(String temperatura){
		
		labelTemperatura.setText(temperatura);
	}
}