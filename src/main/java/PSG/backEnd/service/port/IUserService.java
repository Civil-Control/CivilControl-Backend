package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.security.UserFilterDTO;
import PSG.backEnd.model.dto.security.UserRequestDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for user management.
 */
public interface IUserService {

    /**
     * Creates a new user in the system.
     * @param requestDTO DTO with the user data to create
     * @return DTO of the created user
     */
    UserResponseDTO createUser(UserRequestDTO requestDTO);

    /**
     * Gets all users with filters and pagination.
     * @param filterDTO Optional filters
     * @param pageable Pagination configuration
     * @return Page with the found users
     */
    Page<UserResponseDTO> getAllUsers(UserFilterDTO filterDTO, Pageable pageable);

    /**
     * Gets a user by their ID.
     * @param id User ID
     * @return DTO of the found user
     */
    UserResponseDTO getUserById(Long id);

    /**
     * Updates an existing user.
     * @param id ID of the user to update
     * @param requestDTO DTO with the updated data
     * @return DTO of the updated user
     */
    UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO);

    /**
     * Deletes (soft delete) a user from the system.
     * @param id ID of the user to delete
     */
    void deleteUser(Long id);

    /**
     * Checks if a user exists with the given ID.
     * @param id User ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);
}

