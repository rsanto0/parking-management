package com.estapar.parking.exception;

public class VehicleNotFoundException extends RuntimeException {
    public VehicleNotFoundException(String plate) {
        super("Veiculo não encontrado : " + plate);
    }
}