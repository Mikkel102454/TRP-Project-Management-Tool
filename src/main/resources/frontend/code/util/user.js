let UserDto

async function getUser(){
    if(UserDto !== null){
        try {
            const response = await fetch(`${API_ROOT}/user/whoami`, {
                method: "GET",
                headers: {
                    "Content-Type": "application/json"
                },
            });

            const data = await response.json();

            if(!data.success) return null;
            UserDto = User.fromJson(data);
        } catch (e) {
            log(e, Levels.SEVERE);
            return false;
        }
    }
    return UserDto
}