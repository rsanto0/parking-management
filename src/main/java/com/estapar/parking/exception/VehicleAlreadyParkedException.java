package com.estapar.parking.exception;

public class VehicleAlreadyParkedException extends RuntimeException {
    public VehicleAlreadyParkedException(String licensePlate) {
        super("Veículo " + licensePlate + " já está estacionado");
    }
}