package de.caritas.cob.userservice.api.repository.useragency;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import de.caritas.cob.userservice.api.repository.user.User;

public interface UserAgencyRepository extends CrudRepository<UserAgency, Long> {

  List<UserAgency> findByUser(User user);

}
