package main.requests;

import org.springframework.data.repository.CrudRepository;

public interface RequestCancellationRepository extends CrudRepository<RequestCancellation, Long> {

    Iterable<RequestCancellation> findAllByRequestID(Long requestID);
}
