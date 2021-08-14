import { useState } from "react"
import {
    Card,
    CardHeader,
    CardBody,
    Col,
    Container,
    Row,
    CardColumns,
    CardFooter,
    Form,
    FormGroup,
    Input,
    Label,
} from "reactstrap";
import Service from "../service/Service"

export default function SignUp() {

    const [user, setUser] = useState({
        username: '',
        firstname: '',
        lastname: '',
        email: '',
        password: '',
        avatar: 'default.jpg',
    });

    const [username, setUsername] = useState('');
    const [firstname, setFirstname] = useState('');
    const [lastname, setLastname] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [rePassword, setRePassword] = useState('');

    const onUserNameChange = (e) => {
        setUsername(e.target.value);
    };

    const onFirstnameChange = (e) => {
        setFirstname(e.target.value);
    };

    const onLastnameChange = (e) => {
        setLastname(e.target.value);
    }

    const onEmailChange = (e) => {
        setEmail(e.target.value);
    }

    const onPasswordChange = (e) => {
        setPassword(e.target.value);
    };

    const onRePasswordChange = (e) => {
        setRePassword(e.target.value);
    };

    const onSubmit = (e) => {
        e.preventDefault();
        let user = {
            username,
            firstname,
            lastname,
            email,
            password,
        };
        Service.signup(user);
    }

    // let { username, firstname, lastname, email, password } = user;
    return (
        <div>
            <Row xl="12" sm="12" xs="12" md="12">
                <Col>
                    <Card>
                        <CardHeader>
                            <h3>Đăng ký</h3>
                        </CardHeader>
                        <CardBody>
                        <CardBody>
                            <Form onSubmit={onSubmit}>
                                <FormGroup>
                                    <Label for="username">Tên đăng nhập</Label>
                                    <Input name="username" onChange={onUserNameChange} required/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="firstname">Họ</Label>
                                    <Input name="firstname" onChange={onFirstnameChange} required/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="lastname">Tên</Label>
                                    <Input name="lastname" onChange={onLastnameChange} required/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="email">Email</Label>
                                    <Input name="email" onChange={onEmailChange} required/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="password">Mật khẩu</Label>
                                    <Input name="password" type="password" onChange={onPasswordChange} required/>
                                </FormGroup>
                                <FormGroup>
                                    <Label for="rePassword">Nhập lại mật khẩu</Label>
                                    <Input name="rePassword" type="password" onChange={onRePasswordChange} required/>
                                </FormGroup>
                                <button className="btn btn-primary" type="submit">Đăng ký</button>
                            </Form>
                        </CardBody>
                        </CardBody>
                        <CardFooter>
                            <p>Đã có tài khoản? <a href="/login">Đăng nhập</a></p>
                        </CardFooter>
                    </Card>
                </Col>
            </Row>
        </div>
    )
}