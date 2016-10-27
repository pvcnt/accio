import React from "React";
import {Link} from "react-router";
import {
  Grid,
  Row,
  Col,
  Button,
  Form,
  FormGroup,
  FormControl,
  ControlLabel,
  Radio
} from "react-bootstrap";

let CreateRunForm = React.createClass({
  render: function () {
    return (
      <Grid>
        <h2 className="page-title">
          <img src="images/bars-32px.png"/>
          Create workflow run
        </h2>

        <Form horizontal>
          <FormGroup>
            <Col componentClass={ControlLabel} sm={2}>Run name</Col>
            <Col sm={10}>
              <FormControl type="text" required/>
            </Col>
          </FormGroup>
          <FormGroup>
            <Col componentClass={ControlLabel} sm={2}>Run notes</Col>
            <Col sm={10}>
              <FormControl componentClass="textarea"
                           placeholder="Type some notes to help you remember why you launched this experiment."/>
            </Col>
          </FormGroup>
          <FormGroup>
            <Col componentClass={ControlLabel} sm={2}>Run tags</Col>
            <Col sm={10}>
              <FormControl type="text" placeholder="List of comma-separated tags"/>
            </Col>
          </FormGroup>
          <FormGroup>
            <Col componentClass={ControlLabel} sm={2}>Workflow</Col>
            <Col sm={10}>
              <FormControl.Static>
                <Link to={'workflows/view/' + this.props.workflow.id}>
                  {this.props.workflow.name}
                </Link>
              </FormControl.Static>
            </Col>
          </FormGroup>
          <FormGroup>
            <Col componentClass={ControlLabel} sm={2}>Workflow version</Col>
            <Col sm={10}>
              <Radio defaultChecked>Use last version</Radio>
              <Radio disabled>Use custom version</Radio>
            </Col>
          </FormGroup>
          <Row>
            <Col smOffset={2}>
              <Button bsStyle="primary" type="submit">Start workflow run</Button>
            </Col>
          </Row>
        </Form>
      </Grid>
    );
  }
});

CreateRunForm.propTypes = {
  workflow: React.PropTypes.object.isRequired
};

export default CreateRunForm;