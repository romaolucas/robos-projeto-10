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
	Robot robot;
	Map map;
	Double[][] predictionMatrix;
	double min, max; 
	int numbersegments;
	
	private DiscreteSpace bel;
	
	/*
	Edite as variáveis, modificando com os valores específicos do mapa
	*/
	static private int BOX_DEPTH = 26; // profundidade da caixa
	static private int WALL_DISTANCE = 48; // distância do sonar à parede
	static private int LENGHTMAP = 586; // comprimento (em cm) máximo do mapa
	static private int DISCRET_SIZE = 293; // número de células da discretização
	
	public MainProgram(double mapsize, int numbersegments, Robot robot, Map map) {
		this.robot = robot;
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


		initializeWithUniform(bel);
		initPredictionMatrix();
        printHistogram();
	}

    private void initPredictionMatrix() {
        int n = bel.size();
        predictionMatrix = new Double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    predictionMatrix[i][j] = i == (n - 1) ? 1 : 0.25;
                }
                else if (i == (j + 1)) {
                    predictionMatrix[i][j] = 0.5;
                }
                else if (i == (j + 2)) {
                    predictionMatrix[i][j] = 0.25;
                }
                else {
                    predictionMatrix[i][j] = 0.0;
                }
            }
        }
    }

	private void initializeWithUniform(DiscreteSpace bel) {
		int n = bel.size();
		bel.clear();
		for (int i = 0; i < n; i++) {
			bel.add(1.0 / n);
		}
	}

	private void initializeWithCertainty(DiscreteSpace bel, int posX, double certainty) {
		int n = bel.size();
        double remainingCertainty = 1.0 - certainty;
		double cellCertainty = remainingCertainty / (bel.size() - 1);
		bel.clear();
		for (int i = 0; i < n; i++) {
			if (i == posX) bel.add(certainty);
			else bel.add(cellCertainty);
		} 
	} 

	private void correction (double distance) { 
		/*
			Insira o código de atualização da crença do robô dada uma leitura 'distance' do sonar
		*/
		Boolean sonarBox = distance < 1.2 * (WALL_DISTANCE - BOX_DEPTH);
		Double probSum = 0.0;
		for (int i = 0; i < bel.size(); i++) {
			bel.set(i, bel.get(i) * hasBoxAt(i));
			probSum += bel.get(i) * hasBoxAt(i);
		}
		for (int i = 0; i < bel.size(); i++) {
			bel.set(i, bel.get(i) / probSum);
		}
		printHistogram();
	}

	private Double hasBoxAt(int i) {
		for (Double[] box : map) {
			if (i > box[0] && i < box[1])
				return 1.0;
		}
		return 0.0;
	}

	private void prediction (double delta) {
		/*
			Insira o código de predição da crença do robô dado um deslocamento 'delta'
		*/
		Double probSum = 0.0;
		for (int i = 0; i < bel.size(); i++) {
			Double cellSum = 0.0;
			for (int j = 0; j < bel.size(); j++) {
				cellSum += bel.get(j) * predictionMatrix[i][j];
			}
			bel.set(i, cellSum);
			probSum += cellSum;
		}
		for (int i = 0; i < bel.size(); i++) {
			bel.set(i, bel.get(i) / probSum);
		}
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

		char input = e.getKeyChar();
		switch (input) {
		case 'm': // envia comando de movimento ao robô de uma distância 'dist' inserida pelo usuário
			double dist = askDouble("Distancia (cm)");
			robot.move(dist);
			prediction(dist);
			break;
		case 'r': // reset
			initializeBelief();
			break;
		}
		
		switch (e.getKeyCode()) {
		case KeyEvent.VK_SPACE: // barra de espaco para leitura do sonar 
			robot.read(this);
			break;
		case KeyEvent.VK_UP: // seta cima, mover para frente em 10 cm 
			robot.move(10);
			prediction(10);
			break;
		case KeyEvent.VK_DOWN: // seta baixo, mover para trás em 10 cm
			robot.move(-10);
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
		if (robot == null)
			return;
		robot.disconnect();		
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
			map.add(20, 30); // adiciona uma caixa que inicia que ocupa a posição no eixo-x de 84 a 110 cm
			map.add(101, 131);
			map.add(212, 242);
			map.add(346, 376);
			map.add(422, 452);
			map.add(526, 556);			
		Robot robot =  new Robot("NXT"); // altere para o nome do brick
		if (robot.connect() == false) return;
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new MainProgram(LENGHTMAP, DISCRET_SIZE, robot, map);
			}
		});
	}

	public void robotData(int distance) {
		System.out.println("Distance: "+ distance);
		correction(distance);
	}
}
