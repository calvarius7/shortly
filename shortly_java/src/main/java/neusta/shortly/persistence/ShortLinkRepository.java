package neusta.shortly.persistence;

import neusta.shortly.model.ShortLink;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShortLinkRepository extends CrudRepository<ShortLink, String> {
}
