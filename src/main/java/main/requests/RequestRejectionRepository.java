package main.requests;

import org.springframework.data.repository.CrudRepository;

public interface RequestRejectionRepository extends CrudRepository<RequestRejection, Long> {

    Iterable<RequestRejection> findAllByRequestID(Long requestID);
}
