package model;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import hibernate.Cart;
import hibernate.Course;
import hibernate.User;
import java.util.List;

/**
 * Builds the cart JSON sent to the browser from an explicit field whitelist.
 * Never serialise Hibernate entities directly: the object graph reaches User
 * (password hash, verification code, email) through Cart.user and Course.user.
 *
 * The shape matches what cart.js / checkout.js already read:
 * { id, course: { id, title, description, price, thumbnail, pricelevel, difficulty,
 *                 category: {id, type}, user: {fname, lname} } }
 */
public final class CartJson {

    private CartJson() {
    }

    public static JsonArray toJson(List<Cart> carts) {
        JsonArray arr = new JsonArray();
        if (carts == null) {
            return arr;
        }
        for (Cart cart : carts) {
            JsonObject item = new JsonObject();
            item.addProperty("id", cart.getId());
            Course course = cart.getCourse();
            if (course != null) {
                JsonObject c = new JsonObject();
                c.addProperty("id", course.getId());
                c.addProperty("title", course.getTitle());
                c.addProperty("description", course.getDescription());
                c.addProperty("price", course.getPrice());
                c.addProperty("thumbnail", course.getThumbnail());
                c.addProperty("pricelevel", course.getPricelevel());
                c.addProperty("difficulty", course.getDifficulty());
                if (course.getCategory() != null) {
                    JsonObject cat = new JsonObject();
                    cat.addProperty("id", course.getCategory().getId());
                    cat.addProperty("type", course.getCategory().getType());
                    c.add("category", cat);
                }
                User instructor = course.getUser();
                if (instructor != null) {
                    JsonObject u = new JsonObject();
                    u.addProperty("fname", instructor.getFname());
                    u.addProperty("lname", instructor.getLname());
                    c.add("user", u);
                }
                item.add("course", c);
            }
            arr.add(item);
        }
        return arr;
    }
}
