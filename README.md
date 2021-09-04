# Minisocial Springboot version ![Mini Social-Network](https://github.com/nguyentrinhan-dev/minisocial-spring/workflows/Build%20jar/badge.svg)

### This project is referenced to: [minisocialnetwork](https://github.com/nguyentrinhan-dev/minisocialnetwork).

## Technique
- ### Frontend
    - [React](http://reactjs.org/docs/)
- ### Backend
    - [Spring boot (in studying)](https://spring.io/)
- ### Database
    - [Postgresql](https://www.postgresql.org/docs/)
  
- ### Image storage
    - [Imgur](https://apidocs.imgur.com/)

- ### Others
    - [Liquibase](https://docs.liquibase.com/home.html)
    - [Docker](https://docs.docker.com/)

## To run project
### Database
Create a database name `MiniSocial` (preferably).
### Backend
Import project as maven

Add `application-local.properties` to `src/main/resources` to config database.
```
minisocial.database.host=localhost
minisocial.database.name=miniSocialMedia
spring.datasource.username=<Your database name>
spring.datasource.password=<Your database password>
```
### Third-party
- Create imgur client-id
- Create `./frontend/.env.local` file
- Define `REACT_APP_IMGUR_CLIENTID={your-client-id}` in `.env.local`

### Docker
[Docs](https://www.callicoder.com/spring-boot-spring-security-jwt-mysql-react-app-part-3/)

To run project with this code
- `git clone https://github.com/nguyen-tri-nhan/mini-social-network.git`
- `docker-compose up -d --build`

To run by release
- reference to [docker-compose.yml](/docker-compose.yml)
- replace build step in server by `nguyentrinhan/mini-social-server:release`
- replace build step in client by `nguyentrinhan/mini-social-client:release`




## Feel free to contact me
<p align="center">
	<a href="https://github.com/nguyentrinhan-dev"><img alt="github" width="10%" style="padding:5px" src="https://img.icons8.com/clouds/100/000000/github.png"/></a>
	<a href="https://www.linkedin.com/in/nguyentrinhan-dev/"><img alt="linkedin" width="10%" style="padding:5px" src="https://img.icons8.com/clouds/100/000000/linkedin.png"/></a>
	<a href="https://www.facebook.com/nguyentrinhan.dev/"><img alt="facebook" width="10%" style="padding:5px" src="https://img.icons8.com/clouds/100/000000/facebook-new.png"/></a>
	<a href="https://www.instagram.com/ig.nhan.nguyen/"><img alt="instagram" width="10%" style="padding:5px" src="https://img.icons8.com/clouds/100/000000/instagram.png"/></a>
    <a href="https://www.messenger.com/t/nguyentrinhan.dev/"><img alt="instagram" width="10%" style="padding:5px" src="https://img.icons8.com/clouds/100/000000/facebook-messenger.png"/></a>
</p>