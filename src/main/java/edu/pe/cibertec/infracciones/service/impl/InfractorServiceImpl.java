package edu.pe.cibertec.infracciones.service.impl;

import edu.pe.cibertec.infracciones.dto.InfractorRequestDTO;
import edu.pe.cibertec.infracciones.dto.InfractorResponseDTO;
import edu.pe.cibertec.infracciones.exception.InfractorNotFoundException;
import edu.pe.cibertec.infracciones.exception.VehiculoNotFoundException;
import edu.pe.cibertec.infracciones.model.Infractor;
import edu.pe.cibertec.infracciones.model.Vehiculo;
import edu.pe.cibertec.infracciones.repository.InfractorRepository;
import edu.pe.cibertec.infracciones.repository.VehiculoRepository;
import edu.pe.cibertec.infracciones.service.IInfractorService;
import edu.pe.cibertec.infracciones.repository.MultaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InfractorServiceImpl implements IInfractorService {

    private final InfractorRepository infractorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final MultaRepository multaRepository;

    @Override
    public InfractorResponseDTO registrarInfractor(InfractorRequestDTO dto) {
        Infractor infractor = new Infractor();
        infractor.setDni(dto.getDni());
        infractor.setNombre(dto.getNombre());
        infractor.setApellido(dto.getApellido());
        infractor.setEmail(dto.getEmail());
        infractor.setBloqueado(false);
        return mapToResponse(infractorRepository.save(infractor));
    }

    @Override
    public InfractorResponseDTO obtenerInfractorPorId(Long id) {
        Infractor infractor = infractorRepository.findById(id)
                .orElseThrow(() -> new InfractorNotFoundException(id));
        return mapToResponse(infractor);
    }

    @Override
    public List<InfractorResponseDTO> obtenerTodos() {
        return infractorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void asignarVehiculo(Long infractorId, Long vehiculoId) {
        Infractor infractor = infractorRepository.findById(infractorId)
                .orElseThrow(() -> new InfractorNotFoundException(infractorId));
        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new VehiculoNotFoundException(vehiculoId));
        infractor.getVehiculos().add(vehiculo);
        infractorRepository.save(infractor);
    }

    @Override
    public Double calcularDeuda(Long infractorId) {

        infractorRepository.findById(infractorId)
                .orElseThrow(() -> new InfractorNotFoundException(infractorId));

        var multas = multaRepository.findByInfractorId(infractorId);

        double total = 0;

        for (var m : multas) {
            if (m.getEstado().name().equals("PENDIENTE")) {
                total += m.getMonto();
            } else if (m.getEstado().name().equals("VENCIDA")) {
                total += m.getMonto() * 1.15;
            }
        }

        return total;
    }

    @Override
    public void desasignarVehiculo(Long infractorId, Long vehiculoId) {

        Infractor infractor = infractorRepository.findById(infractorId)
                .orElseThrow(() -> new InfractorNotFoundException(infractorId));

        Vehiculo vehiculo = vehiculoRepository.findById(vehiculoId)
                .orElseThrow(() -> new VehiculoNotFoundException(vehiculoId));

        // Buscar multas del infractor
        var multas = multaRepository.findByInfractorId(infractorId);

        // Validar si hay multas pendientes con ese vehículo
        for (var m : multas) {
            if (m.getVehiculo().getId().equals(vehiculoId)
                    && m.getEstado().name().equals("PENDIENTE")) {
                throw new RuntimeException("No se puede desasignar: tiene multas pendientes");
            }
        }

        // Remover vehículo
        infractor.getVehiculos().remove(vehiculo);

        infractorRepository.save(infractor);
    }


    @Override
    public void transferirMulta(Long multaId, Long nuevoInfractorId) {

        var multa = multaRepository.findById(multaId)
                .orElseThrow(() -> new RuntimeException("Multa no encontrada"));

        var nuevoInfractor = infractorRepository.findById(nuevoInfractorId)
                .orElseThrow(() -> new InfractorNotFoundException(nuevoInfractorId));

        // 1. Validar bloqueado
        if (nuevoInfractor.isBloqueado()) {
            throw new RuntimeException("El infractor está bloqueado");
        }

        // 2. Validar estado de multa
        if (!multa.getEstado().name().equals("PENDIENTE")) {
            throw new RuntimeException("La multa no está pendiente");
        }

        // 3. Validar que tenga el vehículo
        boolean tieneVehiculo = nuevoInfractor.getVehiculos()
                .stream()
                .anyMatch(v -> v.getId().equals(multa.getVehiculo().getId()));

        if (!tieneVehiculo) {
            throw new RuntimeException("El infractor no tiene ese vehículo");
        }

        // Transferir
        multa.setInfractor(nuevoInfractor);

        multaRepository.save(multa);
    }





    private InfractorResponseDTO mapToResponse(Infractor infractor) {
        InfractorResponseDTO dto = new InfractorResponseDTO();
        dto.setId(infractor.getId());
        dto.setDni(infractor.getDni());
        dto.setNombre(infractor.getNombre());
        dto.setApellido(infractor.getApellido());
        dto.setEmail(infractor.getEmail());
        dto.setBloqueado(infractor.isBloqueado());
        return dto;
    }
}