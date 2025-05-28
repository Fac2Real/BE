package com.factoreal.backend.domain.cert.dao;


import com.factoreal.backend.domain.cert.entity.Cert;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertRepository extends JpaRepository<Cert, String> {

}
