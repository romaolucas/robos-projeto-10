import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainProgram extends JPanel implements KeyListener, WindowListener {
	Histogram hist;
	// Robot robot;
	Map map;
	Double[][] predictionMatrix;
	double min, max; 
	int numbersegments;
	int x = 0;
	
	private DiscreteSpace bel;
	
	/*
	Edite as variáveis, modificando com os valores específicos do mapa
	*/
	static private int BOX_DEPTH = 26; // profundidade da caixa
	static private int WALL_DISTANCE = 48; // distância do sonar à parede
	static private int LENGHTMAP = 586; // comprimento (em cm) máximo do mapa
	static private int DISCRET_SIZE = 293; // número de células da discretização
    static private double CEL_SIZE = 2.0;
	
	public MainProgram(double mapsize, int numbersegments, Map map) {
		// this.robot = robot;
		max = mapsize;
		min = 0;
		this.map = map;
		this.numbersegments = numbersegments;
		JFrame frame = new JFrame("Mapa MAC0318");

		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		hist = new Histogram();
		frame.setSize(800, 400);
		frame.setVisible(true);

		frame.setFocusable(true);
		frame.requestFocusInWindow();
		frame.addKeyListener(this);
		frame.addWindowListener(this);

		frame.add(hist);

		initializeBelief(); 
	}

	private void initializeBelief() { 
		bel = new DiscreteSpace();
		for (int i = 0; i < DISCRET_SIZE; i++) {
			bel.add(0.0);
		}

		initializeWithUniform(bel);
        printHistogram();
	}

    private void initPredictionMatrix(double delta) {
        int n = bel.size();
        predictionMatrix = new Double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                predictionMatrix[i][j] = pdf(i, j + delta, 40.0 / CEL_SIZE); 
            }
        }
    }

	private void initializeWithUniform(DiscreteSpace bel) {
		int n = bel.size();
		for (int i = 0; i < n; i++) {
			bel.set(i, (1.0 / n));
		}
	}

	private void initializeWithCertainty(DiscreteSpace bel, int posX, double certainty) {
		int n = bel.size();
        double remainingCertainty = 1.0 - certainty;
		double cellCertainty = remainingCertainty / (bel.size() - 1);
		for (int i = 0; i < n; i++) {
			if (i == posX) bel.set(i, certainty);
			else bel.set(i, cellCertainty);
		} 
	} 

	private void correction (double distance) { 
		/*
			Insira o código de atualização da crença do robô dada uma leitura 'distance' do sonar
		*/
		Boolean sonarBox = distance < 1.2 * (WALL_DISTANCE - BOX_DEPTH);
		for (int i = 0; i < bel.size(); i++) {
			if (sonarBox == hasBoxAt(i)) {
				bel.set(i, bel.get(i));
			} else {
				bel.set(i, 0.0);
			}
		}
		bel.normalize();
		printHistogram();
	}

	private Boolean hasBoxAt(int i) {
		for (Double[] box : map) {
			if (i > box[0] && i < box[1])
				return true;
		}
		return false;
	}

	private void prediction (double delta) {
		/*
			Insira o código de predição da crença do robô dado um deslocamento 'delta'
		*/
		initPredictionMatrix(10);
		DiscreteSpace newBel = new DiscreteSpace();
		for (int i = 0; i < bel.size(); i++) {
			Double cellSum = 0.0;
			for (int j = 0; j < bel.size(); j++) {
				cellSum += bel.get(j) * predictionMatrix[i][j];
			}
			newBel.add(cellSum);
		}
		bel = newBel;
		bel.normalize();
		printHistogram();
	}
	
    public static double pdf(double x) {
        return Math.exp(-x * x / 2) / Math.sqrt(2 * Math.PI); // return pdf(x) = standard Gaussian pdf
    }

    public static double pdf(double x, double mu, double sigma) { 
        return pdf((x - mu) / sigma) / sigma; // return pdf(x, mu, signma) = Gaussian pdf with mean mu and stddev sigma
    }
	
	void printHistogram () {
		hist.print(min, max, bel, map);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		System.out.println("to em: " + x);
		char input = e.getKeyChar();
		switch (input) {
		case 'm': // envia comando de movimento ao robô de uma distância 'dist' inserida pelo usuário
			double dist = askDouble("Distancia (cm)");
			// robot.move(dist);
			prediction(dist);
			break;
		case 'r': // reset
			x = 0;
			initializeBelief();
			break;
		}
		
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE: // barra de espaco para leitura do sonar 
			// robot.read(this);
			double dist;
			System.out.println("tem box?" + hasBoxAt(x));
			if (hasBoxAt(x) == true) {
				dist = 0;

			} else dist = 2 * (WALL_DISTANCE - BOX_DEPTH);
			correction(dist);
			break;
		case KeyEvent.VK_UP: // seta cima, mover para frente em 10 cm 
			// robot.move(10);
			x += 10;
			prediction(10);
			break;
		case KeyEvent.VK_DOWN: // seta baixo, mover para trás em 10 cm
			// robot.move(-10);
			x -= 10;
			prediction(-10);
			break;
		}
	}
	
	private double askDouble(String s) {
		try {
			String rs = JOptionPane.showInputDialog(s);
			return Double.parseDouble(rs);
		} catch (Exception e) {
		}
		return 0;
	}


	@Override
	public void keyReleased(KeyEvent arg0) {		
	}

	@Override
	public void keyTyped(KeyEvent arg0) {	
		char input = arg0.getKeyChar();
		switch (input) {
		case 'i':
			hist.zoomIn();
			break;
		case 'o':
			hist.zoomOut();
			break;
		default:
			break;
		}
		
	}

	@Override
	public void windowActivated(WindowEvent arg0) {		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		System.err.println("Fechando...");
		// if (robot == null)
		// 	return;
		// robot.disconnect();		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {		
	}
	

	public static void main(String[] args) {
		Map map  = new Map();
			// map.add(20, 30); // adiciona uma caixa que inicia que ocupa a posição no eixo-x de 84 a 110 cm
			map.add(101, 131);
			// map.add(212, 242);
			// map.add(346, 376);
			// map.add(422, 452);
			// map.add(526, 556);			
		// Robot robot =  new Robot("NXT"); // altere para o nome do brick
		// if (robot.connect() == false) return;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MainProgram(LENGHTMAP, DISCRET_SIZE, map);
			}
		});
	}

	public void robotData(int distance) {
		System.out.println("Distance: "+ distance);
		correction(distance);
	}
}
