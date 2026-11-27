package ensaio;

import br.edu.unifor.missaomarte.pilotagem.*;

/** Condutor determinístico de validação. Só emite comandos; não modifica estado ou posição. */
public final class PilotoM01 {
    private int fase;
    private int decolagem;
    private double modulacao;
    public Comandos decidir(PartidaDePilotagem partida) {
        NaveEmVoo n = partida.getNave();
        boolean voltando = !n.getABordo().isEmpty();
        if (n.getEstado() == NaveEmVoo.Estado.DESTRUIDA) return Comandos.NENHUM;
        if (n.getEstado() == NaveEmVoo.Estado.POUSADA) {
            if (n.getPlataforma().getTipo() == PlataformaDePouso.Tipo.PASSAGEIRO && !voltando) return Comandos.NENHUM;
            fase = voltando ? 3 : 0;
            decolagem = 36;
            return Comandos.MOTOR;
        }
        if (decolagem > 0) { decolagem--; return Comandos.MOTOR; }
        var destino = voltando ? partida.getCaverna().getBase() : partida.getCaverna().getPlataformas().get(1);
        double x = fase == 0 ? partida.getCaverna().getBase().getCentroX()
                : fase == 3 ? partida.getCaverna().getPlataformas().get(1).getCentroX() : destino.getCentroX();
        double y = 850;
        if (fase == 2 || fase == 5) {
            double altura = n.getY() - (destino.getSuperficie() - partida.getConfig().peY());
            double vx = limitar((x - n.getX()) * 1.5, 35);
            double vy = -Math.max(20, Math.min(100, Math.sqrt(70 * Math.max(0, altura))));
            return conduzir(partida, vx, vy, altura < 100 ? 10 : 25);
        }
        double dx = x - n.getX(), dy = y - n.getY();
        if (Math.hypot(dx, dy) < 25 && Math.hypot(n.getVelocidadeX(), n.getVelocidadeY()) < 25) fase++;
        double vx = limitar(dx * 1.3, Math.min(210, Math.sqrt(120 * Math.abs(dx))));
        double vy = limitar(dy * 1.3, Math.min(115, Math.sqrt(80 * Math.abs(dy)) + 3));
        return conduzir(partida, vx, vy, 45);
    }
    private Comandos conduzir(PartidaDePilotagem p, double vx, double vy, double limite) {
        var n = p.getNave(); var c = p.getConfig();
        double ax = (vx - n.getVelocidadeX()) * 2.2;
        double ay = (vy - n.getVelocidadeY()) * 2.2 + c.gravidade();
        double angulo = limitar(Math.toDegrees(Math.atan2(-ax, Math.max(8, ay))), limite);
        double erro = angulo - n.getAngulo();
        double[] bico = n.direcaoDoBico();
        double fracao = Math.max(0, Math.min(1, (ax * bico[0] + ay * bico[1]) / c.empuxo()));
        if (Math.abs(erro) > 50 && n.getVelocidadeY() > -60) fracao = 0;
        modulacao += fracao;
        boolean motor = modulacao >= 1;
        if (motor) modulacao--;
        return new Comandos(erro > 1.5, erro < -1.5, motor);
    }
    private static double limitar(double valor, double limite) { return Math.max(-limite, Math.min(limite, valor)); }
}
