package edu.pe.cibertec.infracciones.service.impl;

import edu.pe.cibertec.infracciones.model.EstadoMulta;
import edu.pe.cibertec.infracciones.model.Multa;
import edu.pe.cibertec.infracciones.repository.InfractorRepository;
import edu.pe.cibertec.infracciones.repository.MultaRepository;
import edu.pe.cibertec.infracciones.repository.VehiculoRepository;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class T1Test {

    @Mock
    private InfractorRepository infractorRepository;

    @Mock
    private VehiculoRepository vehiculoRepository;

    @Mock
    private MultaRepository multaRepository;

    @InjectMocks
    private InfractorServiceImpl infractorService;

    public T1Test() {
        MockitoAnnotations.openMocks(this);
    }




    @Test
    void calcularDeuda_suma() {

        Long infractorId = 1L;

        Multa m1 = new Multa();
        m1.setMonto(200.0);
        m1.setEstado(EstadoMulta.PENDIENTE);

        Multa m2 = new Multa();
        m2.setMonto(300.0);
        m2.setEstado(EstadoMulta.VENCIDA);

        when(multaRepository.findByInfractorId(infractorId))
                .thenReturn(List.of(m1, m2));

        when(infractorRepository.findById(infractorId))
                .thenReturn(java.util.Optional.of(new edu.pe.cibertec.infracciones.model.Infractor()));

        Double resultado = infractorService.calcularDeuda(infractorId);

        assertEquals(545.0, resultado);
    }



    @Test
    void desasignarVehiculo_sinMultas() {

        Long infractorId = 1L;
        Long vehiculoId = 1L;

        var infractor = new edu.pe.cibertec.infracciones.model.Infractor();
        infractor.setVehiculos(new java.util.ArrayList<>());

        var vehiculo = new edu.pe.cibertec.infracciones.model.Vehiculo();
        vehiculo.setId(vehiculoId);

        infractor.getVehiculos().add(vehiculo);

        when(infractorRepository.findById(infractorId))
                .thenReturn(java.util.Optional.of(infractor));

        when(vehiculoRepository.findById(vehiculoId))
                .thenReturn(java.util.Optional.of(vehiculo));

        when(multaRepository.findByInfractorId(infractorId))
                .thenReturn(List.of());

        infractorService.desasignarVehiculo(infractorId, vehiculoId);

        verify(infractorRepository).save(infractor);
    }





    @Test
    void transferirMulta_correcto() {

        Long multaId = 1L;
        Long nuevoInfractorId = 2L;

        var vehiculo = new edu.pe.cibertec.infracciones.model.Vehiculo();
        vehiculo.setId(1L);

        var multa = new Multa();
        multa.setEstado(EstadoMulta.PENDIENTE);
        multa.setVehiculo(vehiculo);

        var infractor = new edu.pe.cibertec.infracciones.model.Infractor();
        infractor.setBloqueado(false);
        infractor.setVehiculos(new java.util.ArrayList<>());
        infractor.getVehiculos().add(vehiculo);

        when(multaRepository.findById(multaId))
                .thenReturn(java.util.Optional.of(multa));

        when(infractorRepository.findById(nuevoInfractorId))
                .thenReturn(java.util.Optional.of(infractor));

        infractorService.transferirMulta(multaId, nuevoInfractorId);

        verify(multaRepository).save(multa);
    }




    @Test
    void transferirMulta_bloqueado() {

        Long multaId = 1L;
        Long nuevoInfractorId = 2L;

        var vehiculo = new edu.pe.cibertec.infracciones.model.Vehiculo();
        vehiculo.setId(1L);

        var multa = new Multa();
        multa.setEstado(EstadoMulta.PENDIENTE);
        multa.setVehiculo(vehiculo);

        var infractor = new edu.pe.cibertec.infracciones.model.Infractor();
        infractor.setBloqueado(true);
        infractor.setVehiculos(new java.util.ArrayList<>());
        infractor.getVehiculos().add(vehiculo);

        when(multaRepository.findById(multaId))
                .thenReturn(java.util.Optional.of(multa));

        when(infractorRepository.findById(nuevoInfractorId))
                .thenReturn(java.util.Optional.of(infractor));

        assertThrows(RuntimeException.class, () -> {
            infractorService.transferirMulta(multaId, nuevoInfractorId);
        });

        verify(multaRepository, never()).save(any());


    }



}