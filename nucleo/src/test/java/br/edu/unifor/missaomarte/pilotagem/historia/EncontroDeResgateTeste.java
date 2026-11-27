package br.edu.unifor.missaomarte.pilotagem.historia;

import static org.junit.jupiter.api.Assertions.*;
import br.edu.unifor.missaomarte.pilotagem.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class EncontroDeResgateTeste {
    @Test void pulsoConsomeUmaCargaEDesativaPorQuatroSegundos() throws Exception {
        var encontro = EncontroDeResgate.novo();
        posicionar(encontro, 650, 540, -90);
        encontro.atualizar(.05, Comandos.NENHUM, true, false);
        assertEquals(3, encontro.bateria());
        assertEquals(EncontroDeResgate.Estado.DESATIVADO, encontro.estado());
        encontro.atualizar(.1, Comandos.NENHUM, true, false);
        assertEquals(3, encontro.bateria());
        for (int i=0;i<17;i++) encontro.atualizar(.25, Comandos.NENHUM, false, false);
        assertNotEquals(EncontroDeResgate.Estado.DESATIVADO, encontro.estado());
    }

    @Test void tiroSemAlvoTambemConsomeCargaEAssistenciaEsperaAlvo() throws Exception {
        var manual = EncontroDeResgate.novo();
        posicionar(manual, 650, 800, 0);
        manual.atualizar(.05, Comandos.NENHUM, true, false);
        assertEquals(3, manual.bateria());
        var assistido = EncontroDeResgate.novo();
        posicionar(assistido, 650, 800, 0);
        assistido.atualizar(.25, Comandos.NENHUM, false, true);
        assertEquals(4, assistido.bateria());
    }

    @Test void avisoDuraAntesDoPrimeiroProjetil() throws Exception {
        var encontro = EncontroDeResgate.novo();
        posicionar(encontro, 650, 540, 0);
        encontro.atualizar(.25, Comandos.NENHUM, false, false);
        assertEquals(EncontroDeResgate.Estado.AVISO, encontro.estado());
        assertTrue(encontro.projeteis().isEmpty());
    }

    @Test void atualizacaoEmTaxasDiferentesPreservaSimulacao() {
        var a = EncontroDeResgate.novo();
        var b = EncontroDeResgate.novo();
        for (int i=0;i<120;i++) a.atualizar(1.0/120, Comandos.NENHUM, false, false);
        for (int i=0;i<30;i++) b.atualizar(1.0/30, Comandos.NENHUM, false, false);
        assertEquals(a.inimigoX(), b.inimigoX(), .00001);
        assertEquals(a.partida().getTempo(), b.partida().getTempo(), .00001);
    }

    @Test void paredeBloqueiaDeteccaoEPulso() throws Exception {
        var caverna = new Caverna(2400,1200).base(100,128).rocha(700,0,50,1000);
        var encontro = new EncontroDeResgate(new PartidaDePilotagem("Ensaio",
                br.edu.unifor.missaomarte.dominio.Dificuldade.FACIL, caverna, ConfigDeVoo.medio()));
        posicionar(encontro,650,540,-90);
        assertFalse(encontro.alvoNoCone());
        encontro.atualizar(.1,Comandos.NENHUM,true,false);
        assertEquals(EncontroDeResgate.Estado.PATRULHA,encontro.estado());
    }

    @Test void projetilGastaEscudoEImpactosSeguidosTemProtecao() throws Exception {
        var encontro=EncontroDeResgate.novo();
        posicionar(encontro,650,540,0);
        for (int i=0;i<5;i++) encontro.atualizar(.25,Comandos.NENHUM,false,false);
        assertFalse(encontro.projeteis().isEmpty());
        var tiro=encontro.projeteis().get(0);
        posicionar(encontro,tiro.x(),tiro.y(),0);
        encontro.atualizar(PartidaDePilotagem.PASSO,Comandos.NENHUM,false,false);
        assertEquals(1,encontro.escudo());
        assertEquals(1,encontro.impactos());
        assertTrue(encontro.invulneravel()>0);
        posicionar(encontro,encontro.inimigoX(),encontro.inimigoY(),0);
        encontro.atualizar(.1,Comandos.NENHUM,false,false);
        assertEquals(1,encontro.impactos());
        assertEquals(1,encontro.escudo());
        for (int impacto=2;impacto<=3;impacto++) {
            posicionar(encontro,400,850,0);
            for(int i=0;i<5;i++) encontro.atualizar(.25,Comandos.NENHUM,false,false);
            posicionar(encontro,encontro.inimigoX(),encontro.inimigoY(),0);
            encontro.atualizar(PartidaDePilotagem.PASSO,Comandos.NENHUM,false,false);
            assertEquals(impacto,encontro.impactos());
        }
        assertEquals(0,encontro.escudo());
        assertEquals(2,encontro.partida().getVidas());
        assertEquals(NaveEmVoo.Estado.DESTRUIDA,encontro.partida().getNave().getEstado());
    }

    @Test void abortarInterrompeTambemOCombate() {
        var encontro=EncontroDeResgate.novo();
        double antes=encontro.inimigoX();
        encontro.partida().abortar();
        encontro.atualizar(.25,Comandos.NENHUM,true,true);
        assertEquals(antes,encontro.inimigoX());
        assertEquals(4,encontro.bateria());
    }

    static void posicionar(EncontroDeResgate encontro, double x, double y, double angulo) throws Exception {
        var nave = encontro.partida().getNave();
        for (var item : new Object[][]{{"x",x},{"y",y},{"angulo",angulo},
                {"velocidadeX",0.0},{"velocidadeY",0.0},{"estado",NaveEmVoo.Estado.VOANDO},{"plataforma",null}}) {
            Field campo=NaveEmVoo.class.getDeclaredField((String)item[0]);
            campo.setAccessible(true);
            campo.set(nave,item[1]);
        }
    }
}
