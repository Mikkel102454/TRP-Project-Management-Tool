let UserDto = null;
let AllUsers = null
async function getUser(){
    if(UserDto == null){
        try {
            const response = await fetch(`${API_ROOT}/user/whoami`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json"
                },
            });

            const data = await response.json();

            if(!data.success) return null;
            UserDto = User.fromJson(data.data);

        } catch (e) {
            log(e, Levels.SEVERE);
            return false;
        }
    }
    return UserDto
}

async function getAllUsers(){
    if(AllUsers == null){
        try {
            const response = await fetch(`${API_ROOT}/user`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json"
                },
            });

            const data = await response.json();

            if(!data.success) return [];

            let users = []
            for(let user of data.data){
                users.push(User.fromJson(user));
            }
            AllUsers = users;
            return AllUsers;
        } catch (e) {
            log(e, Levels.SEVERE);
            return [];
        }
    }
    return AllUsers
}